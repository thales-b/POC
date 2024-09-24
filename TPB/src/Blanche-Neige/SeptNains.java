// -*- coding: utf-8 -*-

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Thread.*;

public class SeptNains {
    final static BlancheNeige bn = new BlancheNeige();
    final static int nbNains = 7;
    final static String noms [] = {"Simplet", "Dormeur",  "Atchoum", "Joyeux", "Grincheux",
                                   "Prof", "Timide"};

    public static void main(String[] args) throws InterruptedException {
        final Nain nain [] = new Nain [nbNains];
        for(int i = 0; i < nbNains; i++) nain[i] = new Nain(noms[i]);
        for(int i = 0; i < nbNains; i++) nain[i].start();

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            synchronized (bn) {
                bn.notifyAll();
                affiche("Bouscule les nains");
            }
            sleep(100);
        }

        for(int i = 0; i < nbNains; i++) {
            affiche("Interrompt les sept nains");
            nain[i].interrupt();
            nain[i].join();
        }
        affiche("Ils sont mourrus..");
    }

    static class Nain extends Thread {
        public Nain(String nom) {
            this.setName(nom);
        }
        public void run() {
            try {
                while (true) {
                    bn.requérir();
                    bn.accéder();
                    long start = System.currentTimeMillis();
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        long elapsed = System.currentTimeMillis() - start;
                        long remaining = 2000 - elapsed;
                        sleep(remaining);
                        affiche("dit \"J'ai été interrompu, mais maintenant je peux mourru en paix : adieu !\"");
                        break;
                    }
                    bn.relâcher();
                }
            } catch (InterruptedException e) {
                affiche("dit \"Adieu, je suis mourru !\"");
            }
        }
    }

    static void affiche(String message) {
        SimpleDateFormat sdf=new SimpleDateFormat("'['hh'h 'mm'mn 'ss','SSS's] '");
        Date heure = new Date(System.currentTimeMillis());
        System.out.println(sdf.format(heure) + "\"" + Thread.currentThread().getName() + "\" "
                           + message + ".");
    }

    static class BlancheNeige {
        private volatile boolean libre = true;     // Initialement, Blanche-Neige est libre.
        private volatile ArrayDeque<Thread> fileNains = new ArrayDeque<>();

        public synchronized void requérir() {
            affiche("veut un accès exclusif");
            fileNains.addLast(Thread.currentThread());
        }

        public synchronized void accéder() throws InterruptedException {
            while ( ! libre || !fileNains.peekFirst().equals(Thread.currentThread()) ) {
                if (!currentThread().getName().equals("Grincheux")) {
                    wait();                // Le nain attend passivement son tour
                } else {
                    wait(1000);
                    affiche("dit \"Et alors ?\"");
                }
            }
            libre = false;
            affiche("accède à la ressource");
        }

        public synchronized void relâcher() {
            affiche("relâche la ressource");
            fileNains.removeFirst();
            notifyAll();
            libre = true;
        }
    }
}



/*
  % java SeptNains.java
  [10h 51mn 04,406s] "Simplet" veut un accès exclusif.
  [10h 51mn 04,409s] "Simplet" accède à la ressource.
  [10h 51mn 04,409s] "Timide" veut un accès exclusif.
  [10h 51mn 04,409s] "Prof" veut un accès exclusif.
  [10h 51mn 04,409s] "Grincheux" veut un accès exclusif.
  [10h 51mn 04,409s] "Joyeux" veut un accès exclusif.
  [10h 51mn 04,409s] "Atchoum" veut un accès exclusif.
  [10h 51mn 04,409s] "Dormeur" veut un accès exclusif.
  [10h 51mn 06,414s] "Simplet" relâche la ressource.
  [10h 51mn 06,416s] "Simplet" veut un accès exclusif.
  [10h 51mn 06,416s] "Simplet" accède à la ressource.
  [10h 51mn 06,416s] "Timide" accède à la ressource.
  [10h 51mn 06,416s] "Dormeur" accède à la ressource.
  [10h 51mn 06,417s] "Atchoum" accède à la ressource.
  [10h 51mn 06,417s] "Joyeux" accède à la ressource.
  [10h 51mn 06,417s] "Grincheux" accède à la ressource.
  [10h 51mn 06,417s] "Prof" accède à la ressource.
  [10h 51mn 08,417s] "Timide" relâche la ressource.
  [10h 51mn 08,418s] "Timide" veut un accès exclusif.
  [10h 51mn 08,418s] "Timide" accède à la ressource.
  [10h 51mn 08,418s] "Atchoum" relâche la ressource.
  [10h 51mn 08,418s] "Atchoum" veut un accès exclusif.
  [10h 51mn 08,418s] "Atchoum" accède à la ressource.
  ...
*/


/* En remplaçant if par while :
  $ java SeptNains
  ...
*/
