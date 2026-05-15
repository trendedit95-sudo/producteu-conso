package Main;

public class Consommateur implements Runnable {

    private final TamponPartage tamponPartage;
    private final int identifiant;
    private int totalConsomme = 0;

    public Consommateur(TamponPartage tamponPartage, int identifiant) {
        this.tamponPartage = tamponPartage;
        this.identifiant   = identifiant;
    }

    @Override
    public void run() {
        try {
            while (InterfaceGraphique.enCours) {
                // Timeout 300ms — suffisamment court pour reagir vite
                int article = tamponPartage.consommerAvecTimeout(identifiant, 300);
                if (article == -1) {
                    if (InterfaceGraphique.tousProducteursTermines()
                            && tamponPartage.getCount() == 0) {
                        break;
                    }
                    continue;
                }
                totalConsomme++;

                // Delai aleatoire entre 100ms et 800ms
                Thread.sleep(100 + (int)(Math.random() * 700));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            InterfaceGraphique.consommateurTermine();
        }
    }

    public int getIdentifiant()   { return identifiant; }
    public int getTotalConsomme() { return totalConsomme; }
}
