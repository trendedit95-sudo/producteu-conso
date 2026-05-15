package Main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class InterfaceGraphique {

    // ── Etat global ──────────────────────────────────────────────────────────
    public  static volatile boolean enCours            = false;
    private static volatile int     producteursRestants = 0;
    private static volatile int     consommateursActifs = 0;

    // ── Modele ───────────────────────────────────────────────────────────────
    private static TamponPartage              tampon;
    private static final ArrayList<Producteur>   listeProducteurs   = new ArrayList<>();
    private static final ArrayList<Consommateur> listeConsommateurs = new ArrayList<>();
    private static final ArrayList<Thread>       listeThreads       = new ArrayList<>();

    // ── Compteurs ────────────────────────────────────────────────────────────
    private static int totalProduits  = 0;
    private static int totalConsommes = 0;

    // ── Composants UI ────────────────────────────────────────────────────────
    private static JLabel    tailleTamponLabel;
    private static JLabel    totalProduitsLabel;
    private static JLabel    totalConsommesLabel;
    private static JTextArea articlesProduitsTextArea;
    private static JTextArea articlesConsommesTextArea;
    private static JTextArea rapportTextArea;

    private static JButton btnDemarrer;
    private static JButton btnArreter;
    private static JButton btnContinuer;
    private static JButton btnReinitialiser;
    private static JButton btnEnregistrer;

    private static JTextField producteursInput;
    private static JTextField consommateursInput;
    private static JTextField tailleTamponInput;
    private static JTextField articlesAProduireInput;

    private static JFrame frame;

    // ── Visualisation tampon ─────────────────────────────────────────────────
    private static JPanel    tamponVisuelPanel;
    private static JLabel[]  slotLabels;

    private static final Color COULEUR_OCCUPE = new Color(52, 168, 83);   // vert
    private static final Color COULEUR_VIDE   = new Color(220, 220, 220); // gris
    private static final Color COULEUR_TEXTE_OCCUPE = Color.WHITE;
    private static final Color COULEUR_TEXTE_VIDE   = new Color(120, 120, 120);

    // ── Constructeur ─────────────────────────────────────────────────────────
    public InterfaceGraphique() {
        frame = new JFrame("Producteur-Consommateur");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050, 920);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // ── Champs de saisie ─────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nombre de producteurs:"), gbc);
        producteursInput = new JTextField("2");
        gbc.gridx = 1; panel.add(producteursInput, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nombre de consommateurs:"), gbc);
        consommateursInput = new JTextField("2");
        gbc.gridx = 1; panel.add(consommateursInput, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Taille du tampon:"), gbc);
        tailleTamponInput = new JTextField("5");
        gbc.gridx = 1; panel.add(tailleTamponInput, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Nombre d'articles par producteur:"), gbc);
        articlesAProduireInput = new JTextField("10");
        gbc.gridx = 1; panel.add(articlesAProduireInput, gbc);

        // ── Stats en temps reel ──────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Taille actuelle du tampon:"), gbc);
        tailleTamponLabel = new JLabel("0");
        tailleTamponLabel.setFont(tailleTamponLabel.getFont().deriveFont(Font.BOLD));
        tailleTamponLabel.setForeground(Color.BLUE);
        gbc.gridx = 1; panel.add(tailleTamponLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Total articles produits:"), gbc);
        totalProduitsLabel = new JLabel("0");
        totalProduitsLabel.setFont(totalProduitsLabel.getFont().deriveFont(Font.BOLD));
        totalProduitsLabel.setForeground(new Color(0, 150, 0));
        gbc.gridx = 1; panel.add(totalProduitsLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Total articles consommes:"), gbc);
        totalConsommesLabel = new JLabel("0");
        totalConsommesLabel.setFont(totalConsommesLabel.getFont().deriveFont(Font.BOLD));
        totalConsommesLabel.setForeground(Color.RED);
        gbc.gridx = 1; panel.add(totalConsommesLabel, gbc);

        // ── Visualisation tampon en temps reel ───────────────────────────────
        tamponVisuelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tamponVisuelPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 200), 2),
                "  Etat du tampon en temps reel  ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13),
                new Color(60, 60, 180)));
        tamponVisuelPanel.setBackground(new Color(245, 245, 255));
        tamponVisuelPanel.setPreferredSize(new Dimension(600, 90));
        slotLabels = new JLabel[0];
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(tamponVisuelPanel, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        // ── Journaux ─────────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 8;
        panel.add(new JLabel("Journal des produits:"), gbc);
        articlesProduitsTextArea = new JTextArea(8, 35);
        articlesProduitsTextArea.setEditable(false);
        articlesProduitsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        gbc.gridx = 1; panel.add(new JScrollPane(articlesProduitsTextArea), gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        panel.add(new JLabel("Journal des consommations:"), gbc);
        articlesConsommesTextArea = new JTextArea(8, 35);
        articlesConsommesTextArea.setEditable(false);
        articlesConsommesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        gbc.gridx = 1; panel.add(new JScrollPane(articlesConsommesTextArea), gbc);

        // ── Rapport final ─────────────────────────────────────────────────────
        gbc.gridx = 0; gbc.gridy = 10;
        panel.add(new JLabel("Rapport final:"), gbc);
        rapportTextArea = new JTextArea(6, 35);
        rapportTextArea.setEditable(false);
        rapportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        gbc.gridx = 1; panel.add(new JScrollPane(rapportTextArea), gbc);

        // ── Boutons ───────────────────────────────────────────────────────────
        JPanel panneauBoutons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 2;
        panel.add(panneauBoutons, gbc);

        btnDemarrer      = new JButton("▶  Demarrer");
        btnArreter       = new JButton("⏸  Arreter");
        btnContinuer     = new JButton("▶  Continuer");
        btnReinitialiser = new JButton("↺  Reinitialiser");
        btnEnregistrer   = new JButton("💾  Enregistrer rapport");

        styleBtn(btnDemarrer,      new Color(0, 150, 0));
        styleBtn(btnArreter,       new Color(200, 50, 50));
        styleBtn(btnContinuer,     new Color(0, 100, 200));
        styleBtn(btnReinitialiser, new Color(120, 60, 0));
        styleBtn(btnEnregistrer,   new Color(60, 60, 180));

        // Taille plus grande pour les boutons principaux
        btnDemarrer.setPreferredSize(new Dimension(150, 40));
        btnArreter.setPreferredSize(new Dimension(150, 40));
        btnContinuer.setPreferredSize(new Dimension(150, 40));
        btnReinitialiser.setPreferredSize(new Dimension(160, 40));
        btnEnregistrer.setPreferredSize(new Dimension(200, 40));

        panneauBoutons.add(btnDemarrer);
        panneauBoutons.add(btnArreter);
        panneauBoutons.add(btnContinuer);
        panneauBoutons.add(btnReinitialiser);
        panneauBoutons.add(btnEnregistrer);

        modeInitial();

        // ── Actions ───────────────────────────────────────────────────────────

        // DEMARRER
        btnDemarrer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int nbProd     = Integer.parseInt(producteursInput.getText().trim());
                    int nbCons     = Integer.parseInt(consommateursInput.getText().trim());
                    int taille     = Integer.parseInt(tailleTamponInput.getText().trim());
                    int nbArticles = Integer.parseInt(articlesAProduireInput.getText().trim());

                    if (nbProd <= 0 || nbCons <= 0 || taille <= 0 || nbArticles <= 0) {
                        JOptionPane.showMessageDialog(frame, "Les valeurs doivent etre positives.");
                        return;
                    }

                    totalProduits  = 0;
                    totalConsommes = 0;
                    totalProduitsLabel.setText("0");
                    totalConsommesLabel.setText("0");
                    articlesProduitsTextArea.setText("");
                    articlesConsommesTextArea.setText("");
                    rapportTextArea.setText("");
                    listeProducteurs.clear();
                    listeConsommateurs.clear();
                    listeThreads.clear();

                    tampon = new TamponPartage(taille);
                    tailleTamponLabel.setText("0");
                    initialiserSlots(taille);

                    for (int i = 1; i <= nbProd; i++) {
                        listeProducteurs.add(new Producteur(tampon, i, nbArticles));
                    }
                    producteursRestants = nbProd;
                    consommateursActifs = nbCons;
                    enCours = true;

                    lancerThreads(nbCons);
                    modeEnCours();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Veuillez entrer des valeurs entieres valides.");
                }
            }
        });

        // ARRETER
        btnArreter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enCours = false;
                for (Thread t : listeThreads) t.interrupt();
                listeThreads.clear();
                afficherRapport("ARRET MANUEL");
                modeArrete();
            }
        });

        // CONTINUER
        btnContinuer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enCours = true;
                listeThreads.clear();

                int nbProdRelances = 0;
                for (Producteur p : listeProducteurs) {
                    if (p.getDejaFait() < p.getArticlesAProduire()) {
                        Thread t = new Thread(p, "Producteur-" + p.getIdentifiant());
                        listeThreads.add(t);
                        t.start();
                        nbProdRelances++;
                    }
                }

                if (nbProdRelances == 0) {
                    producteursRestants = 0;
                } else {
                    producteursRestants = nbProdRelances;
                }

                int nbCons = 0;
                try {
                    nbCons = Integer.parseInt(consommateursInput.getText().trim());
                } catch (NumberFormatException ex) { nbCons = 1; }
                consommateursActifs = nbCons;
                lancerConsommateurs(nbCons);

                articlesProduitsTextArea.append("--- REPRISE ---\n");
                articlesConsommesTextArea.append("--- REPRISE ---\n");
                modeEnCours();
            }
        });

        // REINITIALISER
        btnReinitialiser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enCours = false;
                for (Thread t : listeThreads) t.interrupt();
                listeThreads.clear();
                listeProducteurs.clear();
                listeConsommateurs.clear();
                tampon = null;

                totalProduits  = 0;
                totalConsommes = 0;
                totalProduitsLabel.setText("0");
                totalConsommesLabel.setText("0");
                tailleTamponLabel.setText("0");
                articlesProduitsTextArea.setText("");
                articlesConsommesTextArea.setText("");
                rapportTextArea.setText("");
                viderSlots();
                modeInitial();
            }
        });

        // ENREGISTRER
        btnEnregistrer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enregistrerRapport();
            }
        });

        frame.getContentPane().add(new JScrollPane(panel));
        frame.setVisible(true);
    }

    // ── Initialisation et mise a jour des slots visuels ───────────────────────
    private static void initialiserSlots(int taille) {
        tamponVisuelPanel.removeAll();
        slotLabels = new JLabel[taille];
        for (int i = 0; i < taille; i++) {
            JLabel slot = new JLabel("vide", SwingConstants.CENTER);
            slot.setPreferredSize(new Dimension(80, 55));
            slot.setOpaque(true);
            slot.setBackground(COULEUR_VIDE);
            slot.setForeground(COULEUR_TEXTE_VIDE);
            slot.setFont(new Font("Monospaced", Font.BOLD, 11));
            slot.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            slot.setText("<html><center>slot " + i + "<br><b>vide</b></center></html>");
            slotLabels[i] = slot;
            tamponVisuelPanel.add(slot);
        }
        tamponVisuelPanel.revalidate();
        tamponVisuelPanel.repaint();
    }

    private static void viderSlots() {
        tamponVisuelPanel.removeAll();
        slotLabels = new JLabel[0];
        tamponVisuelPanel.revalidate();
        tamponVisuelPanel.repaint();
    }

    // Mise a jour visuelle de tous les slots selon l'etat reel du tampon
    public static void mettreAJourTampon() {
        if (tampon == null || slotLabels == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int count  = tampon.getCount();
                int taille = tampon.getTailleMax();
                int outIdx = tampon.getOut();
                int[] contenu = tampon.getContenu();

                tailleTamponLabel.setText(count + " / " + taille);
                totalProduitsLabel.setText(String.valueOf(totalProduits));
                totalConsommesLabel.setText(String.valueOf(totalConsommes));

                // Reconstruire l'affichage: slots occupes depuis out
                for (int i = 0; i < slotLabels.length; i++) {
                    int slotPhysique = (outIdx + i) % taille;
                    if (i < count) {
                        // slot occupe
                        int valeur = contenu[slotPhysique];
                        slotLabels[i].setBackground(COULEUR_OCCUPE);
                        slotLabels[i].setForeground(COULEUR_TEXTE_OCCUPE);
                        slotLabels[i].setText(
                            "<html><center><small>slot " + i + "</small><br><b>" + valeur + "</b></center></html>");
                    } else {
                        // slot vide
                        slotLabels[i].setBackground(COULEUR_VIDE);
                        slotLabels[i].setForeground(COULEUR_TEXTE_VIDE);
                        slotLabels[i].setText(
                            "<html><center><small>slot " + i + "</small><br>vide</center></html>");
                    }
                }
            }
        });
    }

    // Ancienne methode gardee pour compatibilite
    public static void mettreAJourTailleTampon() {
        mettreAJourTampon();
    }

    // ── Lancement des threads ─────────────────────────────────────────────────
    private static void lancerThreads(int nbCons) {
        for (Producteur p : listeProducteurs) {
            Thread t = new Thread(p, "Producteur-" + p.getIdentifiant());
            listeThreads.add(t);
            t.start();
        }
        lancerConsommateurs(nbCons);
    }

    private static void lancerConsommateurs(int nbCons) {
        listeConsommateurs.clear();
        for (int i = 1; i <= nbCons; i++) {
            Consommateur c = new Consommateur(tampon, i);
            listeConsommateurs.add(c);
            Thread t = new Thread(c, "Consommateur-" + i);
            listeThreads.add(t);
            t.start();
        }
    }

    // ── Gestion etats boutons ─────────────────────────────────────────────────
    private static void modeInitial() {
        btnDemarrer.setVisible(true);
        btnArreter.setVisible(false);
        btnContinuer.setVisible(false);
        btnReinitialiser.setVisible(false);
        btnEnregistrer.setVisible(false);
        setChamps(true);
    }

    private static void modeEnCours() {
        btnDemarrer.setVisible(false);
        btnArreter.setVisible(true);
        btnContinuer.setVisible(false);
        btnReinitialiser.setVisible(true);
        btnEnregistrer.setVisible(false);
        setChamps(false);
    }

    private static void modeArrete() {
        btnDemarrer.setVisible(false);
        btnArreter.setVisible(false);
        btnContinuer.setVisible(true);
        btnReinitialiser.setVisible(true);
        btnEnregistrer.setVisible(true);
        setChamps(false);
    }

    private static void modeTermine() {
        btnDemarrer.setVisible(false);
        btnArreter.setVisible(false);
        btnContinuer.setVisible(false);
        btnReinitialiser.setVisible(true);
        btnEnregistrer.setVisible(true);
        setChamps(false);
    }

    private static void setChamps(boolean actif) {
        producteursInput.setEnabled(actif);
        consommateursInput.setEnabled(actif);
        tailleTamponInput.setEnabled(actif);
        articlesAProduireInput.setEnabled(actif);
    }

    private static void styleBtn(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setFocusPainted(false);
    }

    // ── Rapport ───────────────────────────────────────────────────────────────
    private static void afficherRapport(String raison) {
        if (tampon == null) return;
        String heure = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("  RAPPORT (" + raison + ") — " + heure + "\n");
        sb.append("================================\n");
        sb.append("Total produit  : " + tampon.getTotalDepose() + "\n");
        sb.append("Total consomme : " + tampon.getTotalRetire() + "\n");
        sb.append("Restant tampon : " + tampon.getCount() + "\n");
        sb.append("--------------------------------\n");
        for (Producteur p : listeProducteurs) {
            sb.append("Producteur  " + p.getIdentifiant()
                    + " → " + p.getTotalProduit() + " articles"
                    + "  (avancement: " + p.getDejaFait()
                    + "/" + p.getArticlesAProduire() + ")\n");
        }
        for (Consommateur c : listeConsommateurs) {
            sb.append("Consommateur " + c.getIdentifiant()
                    + " → " + c.getTotalConsomme() + " articles\n");
        }
        sb.append("================================\n");
        rapportTextArea.setText(sb.toString());
    }

    // ── Enregistrer dans fichier ──────────────────────────────────────────────
    private static void enregistrerRapport() {
        String heure = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH-mm-ss"));
        String nomFichier = "rapport_" + heure + ".txt";
        try (FileWriter fw = new FileWriter(nomFichier)) {
            fw.write("=== JOURNAL DES PRODUCTIONS ===\n");
            fw.write(articlesProduitsTextArea.getText());
            fw.write("\n=== JOURNAL DES CONSOMMATIONS ===\n");
            fw.write(articlesConsommesTextArea.getText());
            fw.write("\n=== RAPPORT FINAL ===\n");
            fw.write(rapportTextArea.getText());
            JOptionPane.showMessageDialog(frame,
                    "Rapport enregistre : " + nomFichier + " OK");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Erreur lors de l'enregistrement !");
        }
    }

    // ── Appele par Producteur quand il a fini tous ses articles ───────────────
    public static synchronized void producteurTermine() {
        producteursRestants--;
    }

    // ── Appele par Consommateur dans son finally ───────────────────────────────
    public static synchronized void consommateurTermine() {
        consommateursActifs--;
        if (consommateursActifs <= 0 && producteursRestants <= 0 && enCours) {
            enCours = false;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    afficherRapport("FIN NORMALE");
                    modeTermine();
                }
            });
        }
    }

    public static boolean tousProducteursTermines() {
        return producteursRestants <= 0;
    }

    // ── Mises a jour UI ───────────────────────────────────────────────────────
    public static void ajouterArticleProduit(String ligne, int idProducteur) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                totalProduits++;
                totalProduitsLabel.setText(String.valueOf(totalProduits));
                articlesProduitsTextArea.append(ligne + "\n");
                articlesProduitsTextArea.setCaretPosition(
                        articlesProduitsTextArea.getDocument().getLength());
            }
        });
    }

    public static void ajouterArticleConsomme(String ligne, int idConsommateur) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                totalConsommes++;
                totalConsommesLabel.setText(String.valueOf(totalConsommes));
                articlesConsommesTextArea.append(ligne + "\n");
                articlesConsommesTextArea.setCaretPosition(
                        articlesConsommesTextArea.getDocument().getLength());
            }
        });
    }
}
