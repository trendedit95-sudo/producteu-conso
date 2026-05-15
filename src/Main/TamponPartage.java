package Main;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TamponPartage {

    private final int[] tampon;
    private int Count, in, out;
    private final Semaphore vide;
    private final Semaphore plein;
    private final Semaphore mutex;

    private int totalDepose = 0;
    private int totalRetire = 0;

    static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public TamponPartage(int taille) {
        tampon = new int[taille];
        this.Count = in = out = 0;
        vide  = new Semaphore(taille);
        plein = new Semaphore(0);
        mutex = new Semaphore(1);
    }

    public void produire(int article, int idProducteur)
            throws InterruptedException {
        vide.acquire();
        mutex.acquire();
        try {
            tampon[in] = article;
            in = (in + 1) % tampon.length;
            Count++;
            totalDepose++;
            String heure = LocalDateTime.now().format(FORMAT);
            String ligne = "[" + heure + "] Producteur " + idProducteur
                    + " a produit : " + article;
            InterfaceGraphique.ajouterArticleProduit(ligne, idProducteur);
            InterfaceGraphique.mettreAJourTampon();
        } finally {
            mutex.release();
            plein.release();
        }
    }

    public int consommerAvecTimeout(int idConsommateur, long timeoutMs)
            throws InterruptedException {
        boolean disponible = plein.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        if (!disponible) return -1;
        mutex.acquire();
        try {
            int article = tampon[out];
            out = (out + 1) % tampon.length;
            Count--;
            totalRetire++;
            String heure = LocalDateTime.now().format(FORMAT);
            String ligne = "[" + heure + "] Consommateur " + idConsommateur
                    + " a consomme : " + article;
            InterfaceGraphique.ajouterArticleConsomme(ligne, idConsommateur);
            InterfaceGraphique.mettreAJourTampon();
            return article;
        } finally {
            mutex.release();
            vide.release();
        }
    }

    // Retourne un snapshot du contenu actuel du tampon circulaire
    public synchronized int[] getContenu() {
        int[] snapshot = new int[tampon.length];
        for (int i = 0; i < tampon.length; i++) {
            snapshot[i] = tampon[i];
        }
        return snapshot;
    }

    public int getCount()       { return Count; }
    public int getTotalDepose() { return totalDepose; }
    public int getTotalRetire() { return totalRetire; }
    public int getTailleMax()   { return tampon.length; }
    public int getIn()          { return in; }
    public int getOut()         { return out; }
}
