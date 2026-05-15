package Main;

public class Producteur implements Runnable {

    private final TamponPartage tamponPartage;
    private final int identifiant;
    private final int articlesAProduire;
    private int dejaFait = 0;
    private int totalProduit = 0;

    public Producteur(TamponPartage tamponPartage, int identifiant,
                      int articlesAProduire) {
        this.tamponPartage     = tamponPartage;
        this.identifiant       = identifiant;
        this.articlesAProduire = articlesAProduire;
    }

    @Override
    public void run() {
        try {
            for (int i = dejaFait;
                 i < articlesAProduire && InterfaceGraphique.enCours; i++) {

                int article = identifiant * 100 + i;
                tamponPartage.produire(article, identifiant);
                dejaFait = i + 1;
                totalProduit++;

                // Delai aleatoire entre 100ms et 800ms — assez court pour
                // que producteurs et consommateurs tournent vraiment en parallele
                Thread.sleep(100 + (int)(Math.random() * 700));
            }
            if (dejaFait >= articlesAProduire) {
                InterfaceGraphique.producteurTermine();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getDejaFait()          { return dejaFait; }
    public int getIdentifiant()       { return identifiant; }
    public int getArticlesAProduire() { return articlesAProduire; }
    public int getTotalProduit()      { return totalProduit; }
}
