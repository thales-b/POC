import javax.swing.*;

import static java.lang.Thread.currentThread;

public class MandelbrotThread implements Runnable {
    private final int startRow;
    private final int endRow;

    public MandelbrotThread(int startRow, int endRow) {
        this.startRow = startRow;
        this.endRow = endRow;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        for (int col = 0; col < Mandelbrot.taille; col++) {
            for (int row = startRow; row < endRow; row++) {
                Mandelbrot.colorierPixel(col, row);
            }
            Mandelbrot.showImage();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println(currentThread().getName() + " pour les lignes de " + startRow + " à " + endRow + " a mis " + duration + " ms.");
    }
}
