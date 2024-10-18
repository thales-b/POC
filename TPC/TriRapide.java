package Tri_rapide;// -*- coding: utf-8 -*-

import java.util.Arrays;
import java.util.Random ;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static Tri_rapide.TriRapide.seuil;

public class TriRapide {
    static final int taille = 80_000_000 ;                   // Longueur du tableau à trier
    static final int [] tableau = new int[taille] ;         // Le tableau d'entiers à trier
    static final int borne = 10 * taille ;                  // Valeur maximale dans le tableau
    static final int seuil = taille / 100;
    static final int nbThreads = 4;

    private static void echangerElements(int[] t, int m, int n) {
        int temp = t[m] ;
        t[m] = t[n] ;
        t[n] = temp ;
    }

    static int partitionner(int[] t, int début, int fin) {
        int v = t[fin] ;                               // Choix (arbitraire) du pivot : t[fin]
        int place = début ;                            // Place du pivot, à droite des éléments déplacés
        for (int i = début ; i<fin ; i++) {            // Parcours du *reste* du tableau
            if (t[i] < v) {                            // Cette valeur t[i] doit être à droite du pivot
                echangerElements(t, i, place) ;        // On le place à sa place
                place++ ;                              // On met à jour la place du pivot
            }
        }
        echangerElements(t, place, fin) ;              // Placement définitif du pivot
        return place ;
    }

    static void trierRapidement(int[] t, int début, int fin) {
        if (début < fin) {                             // S'il y a un seul élément, il n'y a rien à faire!
            int p = partitionner(t, début, fin) ;
            trierRapidement(t, début, p-1) ;
            trierRapidement(t, p+1, fin) ;
        }
    }

    private static void afficher(int[] t, int début, int fin) {
        for (int i = début ; i <= début+3 ; i++) {
            System.out.print(" " + t[i]) ;
        }
        System.out.print("...") ;
        for (int i = fin-3 ; i <= fin ; i++) {
            System.out.print(" " + t[i]) ;
        }
        System.out.println() ;
    }

    public static void main(String[] args) {
        Random aléa = new Random() ;
        for (int i=0 ; i<taille ; i++) {                          // Remplissage aléatoire du tableau
            tableau[i] = aléa.nextInt(2*borne) - borne ;
        }
        System.out.print("Tableau initial : ") ;
        afficher(tableau, 0, taille - 1) ;                         // Affiche le tableau à trier

        // Copier les tableaux pour vérifier la cohérence du tri
        int[] copieThreadpool = Arrays.copyOf(tableau, tableau.length) ;
        int[] copieForkJoin = Arrays.copyOf(tableau, tableau.length) ;

        // Créer les différentes classes de stratégie de tri
        TriRapideSéquentiel séquentiel = new TriRapideSéquentiel(tableau) ;
        TriRapidePool parallèlePool = new TriRapidePool(copieThreadpool) ;
        TriRapideForkJoin parallèleForkJoin  = new TriRapideForkJoin(copieForkJoin) ;

        long duréeSequentielle = chronométrer(séquentiel);
        System.out.println();

        // Calculer la durée pour tous les types de tris, et les gains des tris parallèles
        long duréeParallèlePool = chronométrer(parallèlePool);
        double gainParallèlePool = ((double) duréeSequentielle / duréeParallèlePool);
        System.out.println("Gain observé : " + gainParallèlePool + "\n");

        long duréeParallèleForkJoin = chronométrer(parallèleForkJoin);
        double gainParallèleForkJoin = ((double) duréeSequentielle / duréeParallèleForkJoin);
        System.out.println("Gain observé : " + gainParallèleForkJoin + "\n");

        // Vérifier l'égalité entre tous les tableaux triés
        if (Arrays.equals(tableau, copieThreadpool) && Arrays.equals(copieThreadpool, copieForkJoin)) {
            System.out.println("Les tris sont cohérents.");
        }
    }

    public static long chronométrer(StratégieTriRapide stratégie) {
        long début = System.nanoTime();
        stratégie.trier();                   // Tri du tableau
        long fin = System.nanoTime();
        long durée = (fin - début) / 1_000_000 ;

        System.out.println(stratégie + " " + durée + " ms.");

        System.out.print("Tableau trié : ") ;
        afficher(stratégie.getTableau(), 0, stratégie.getTableau().length -1) ;                         // Affiche le tableau obtenu

        return durée;
    }
}

interface StratégieTriRapide {
    void trier();
    int[] getTableau() ;
}

class TriRapideSéquentiel implements StratégieTriRapide {
    private final int [] tab;

    public TriRapideSéquentiel(int[] tableau) {
        this.tab = tableau;
    }

    @Override
    public int[] getTableau() {
        return this.tab;
    }

    public void trier() {
        TriRapide.trierRapidement(this.tab, 0, this.tab.length - 1);
    }

    @Override
    public String toString() {
        return "Tri séquentiel :";
    }
}

class TriRapidePool implements StratégieTriRapide {
    private final int [] tableau;
    private ExecutorService exécuteur = Executors.newFixedThreadPool(TriRapide.nbThreads);

    public TriRapidePool(int[] tableau) {
        this.tableau = tableau;
    }
    public void trier() {
        // Compteur de tâches pour suivre l'évolution du nombre de tâches
        AtomicInteger compteurTâches = new AtomicInteger(1);
        exécuteur.submit(new TâcheTriRapidePool(tableau, 0, tableau.length - 1, exécuteur, compteurTâches));

        synchronized (compteurTâches) {
            while (compteurTâches.get() > 0) {
                try {
                    compteurTâches.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Arrêt d'acceptation des tâches après la fin des soumissions de tâches
        exécuteur.shutdown();
    }

    @Override
    public int[] getTableau() {
        return this.tableau;
    }

    @Override
    public String toString() {
        return "Tri parallèle avec ThreadPool (Exercice C.3) :";
    }
}

class TâcheTriRapidePool implements Callable<Void> {
    private final int[] tableau;
    private final int début;
    private final int fin;
    private final ExecutorService exécuteur;
    private final AtomicInteger compteurTâches;

    public TâcheTriRapidePool(int[] tableau, int début, int fin, ExecutorService exécuteur, AtomicInteger compteurTâches) {
        this.tableau = tableau;
        this.début = début;
        this.fin = fin;
        this.exécuteur = exécuteur;
        this.compteurTâches = compteurTâches;
    }

    public Void call() {
        try {
            if (fin - début + 1 <= seuil) {
                TriRapide.trierRapidement(tableau, début, fin);
            } else {
                int pivot = TriRapide.partitionner(tableau, début, fin);

                compteurTâches.incrementAndGet();
                exécuteur.submit(new TâcheTriRapidePool(tableau, début, pivot - 1, exécuteur, compteurTâches));

                compteurTâches.incrementAndGet();
                exécuteur.submit(new TâcheTriRapidePool(tableau, pivot + 1, fin, exécuteur, compteurTâches));
            }
        } finally {
            // Une fois que toutes les tâches ont été soumises, permettre le shutdown
            synchronized (compteurTâches) {
                if (compteurTâches.decrementAndGet() == 0) {
                    compteurTâches.notifyAll();
                }
            }
        }
        return null;
    }
}

class TriRapideForkJoin implements StratégieTriRapide {
    private final int [] tableau;
    private ForkJoinPool exécuteur = new ForkJoinPool(TriRapide.nbThreads);

    public TriRapideForkJoin(int[] tableau) {
        this.tableau = tableau;
    }

    public void trier() {
        exécuteur.invoke(new TâcheTriRapideForkJoin(tableau, 0, tableau.length - 1));
        exécuteur.shutdown();
    }

    @Override
    public int[] getTableau() {
        return this.tableau;
    }

    @Override
    public String toString() {
        return "Tri parallèle avec ForkJoinPool (Exercice E.2):";
    }
}

class TâcheTriRapideForkJoin extends RecursiveAction {
    private final int[] tableau;
    private final int début;
    private final int fin;

    public TâcheTriRapideForkJoin(int[] tableau, int début, int fin) {
        this.tableau = tableau;
        this.début = début;
        this.fin = fin;
    }

    public void compute() {
        if (fin - début + 1 <= seuil) {
            TriRapide.trierRapidement(tableau, début, fin);
        } else {
            int pivot = TriRapide.partitionner(tableau, début, fin);

            TâcheTriRapideForkJoin t1 = new TâcheTriRapideForkJoin(tableau, début, pivot - 1);
            TâcheTriRapideForkJoin t2 = new TâcheTriRapideForkJoin(tableau, pivot + 1, fin);

            t2.fork();
            t1.compute();
            t2.join();
        }
    }
}