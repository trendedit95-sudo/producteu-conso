# Makefile — Producteur-Consommateur

# Variables
JAVAC      = javac
JAVA       = java
SRC_DIR    = src/Main
OUT_DIR    = .
MAIN_CLASS = Main.App

# Fichiers source
SOURCES = $(SRC_DIR)/TamponPartage.java \
          $(SRC_DIR)/Producteur.java    \
          $(SRC_DIR)/Consommateur.java  \
          $(SRC_DIR)/InterfaceGraphique.java \
          $(SRC_DIR)/App.java

# Regles
all: compile

compile:
	$(JAVAC) -d $(OUT_DIR) $(SOURCES)

run: compile
	$(JAVA) -cp $(OUT_DIR) $(MAIN_CLASS)

clean:
	del /Q Main\*.class 2>nul || rm -f Main/*.class
