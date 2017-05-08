
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FiltreAntiSpam {
	
	public static final double epsilon = 0.5;
	
	public int[] nbSpam;
	public int[] nbHam;
	
	public double[] bSpam;
	public double[] bHam;
	
	public double PSpam;
	public double PHam;
	
	public int NSpam;
	public int NHam;
	public int NMail;

	private ArrayList<String> dictionnaire;

	public FiltreAntiSpam() throws Exception {
		charger_dictionnaire();
	}

	public FiltreAntiSpam(String fichier) throws Exception {
		charger_dictionnaire(fichier);
	}

	public void charger_dictionnaire() throws Exception{
		charger_dictionnaire("./res/dictionnaire1000en.txt");
	}
	
	public void charger_dictionnaire(String fichier) throws Exception {
		dictionnaire = new ArrayList<>();
		String ligne;
		System.out.println("Chargement du dictionnaire : "+fichier);
		InputStream is=new FileInputStream(fichier); 
		InputStreamReader isr=new InputStreamReader(is);
		BufferedReader br=new BufferedReader(isr);	
		while ((ligne=br.readLine())!=null){	
			// On ne charge que les mots d'au moins trois lettres, ceux de deux sont généralement des pronoms peu impactant sur le classement car omniprésents.
			if(ligne.length() >= 3){
				dictionnaire.add(ligne);
			}
		}
		br.close(); 
		System.out.println("Chargement du dictionnaire terminé !");
	}
	
	public boolean[] vecteur_occurence(String fichier) throws Exception {
		int taille = dictionnaire.size();
		boolean mots[] = new boolean[taille];
		for(int i = 0; i < mots.length;i++){
			mots[i] = false;
		}
		
		String ligne;
		int index;
		String sac[];
		String regex = "[^A-Za-z]";
		InputStream is=new FileInputStream(fichier); 
		InputStreamReader isr=new InputStreamReader(is);
		BufferedReader br=new BufferedReader(isr);
		
		while ((ligne=br.readLine())!=null){
			sac = ligne.split(regex);
			for(String mot : sac){
				//On supprime les mots de moins de 3 lettres, les pronoms ne sont pas intéressants.
				if(mot.length()>=3){
					mot = mot.toUpperCase();
					if(dictionnaire.contains(mot)){
						index = dictionnaire.indexOf(mot);
						mots[index] = true;
					}
				}
			}
		}
		br.close(); 
		return mots;
	}
	
	public void charger_apprentissage() throws Exception {
		charger_apprentissage("./res/mon_classifieur.txt");
	}
	
	public void charger_apprentissage(String classifieurPath) throws Exception {
		System.out.println("Chargement de la base d'apprentissage : "+classifieurPath);
		int taille = dictionnaire.size();
		InputStream is=new FileInputStream(classifieurPath); 
		InputStreamReader isr=new InputStreamReader(is);
		BufferedReader br=new BufferedReader(isr);
		String line, mots[];
		String regex = "\\|";
		line = br.readLine();
		mots = line.split(regex);
		NMail = Integer.parseInt(mots[0]);
		NSpam = Integer.parseInt(mots[1]);
		NHam = Integer.parseInt(mots[2]);
		PSpam = (double) ( (double) NSpam / (double) NMail) ;
		PHam = (double) ( (double) NHam / (double) NMail) ;
		nbSpam = new int[taille];
		nbHam = new int[taille];
		bSpam = new double[taille];
		bHam = new double[taille];
		line = br.readLine();
		mots = line.split(regex);
		for (int i = 0; i < taille; i++)
		{
			nbSpam[i] = Integer.parseInt(mots[i]);
			bSpam[i] = (double) ( (double) (nbSpam[i] + epsilon) / (double) (NSpam + 2*epsilon) );
		}
		line = br.readLine();
		mots = line.split(regex);
		for (int i = 0; i < taille; i++)
		{
			nbHam[i] = Integer.parseInt(mots[i]);
			bHam[i] = (double) ( (double) (nbHam[i] + epsilon) / (double) (NHam + 2*epsilon) );
		}
		br.close();
		System.out.println("Chargement de la base d'apprentissage effectué avec succès !");
	}
	
	public void sauvegarder_apprentissage() {
		sauvegarder_apprentissage("./res/mon_classifieur.txt");
	}
	public void sauvegarder_apprentissage(String classifieur) {
		int taille = dictionnaire.size();
		try {
			OutputStream os = new FileOutputStream(classifieur);
			OutputStreamWriter osw = new OutputStreamWriter(os);

			osw.write(NMail+"|"+NSpam+"|"+NHam+"|\n");
			StringBuilder sbspam = new StringBuilder(), sbham = new StringBuilder();
			for (int i = 0; i < taille; i++){
				String sp = Integer.toString(nbSpam[i]);
				String h = Integer.toString(nbHam[i]);
				sbspam.append(sp+"|");
				sbham.append(h+"|");
			}
			sbspam.append("\n");
			sbham.append("\n");
			osw.write(sbspam.toString());
			osw.write(sbham.toString());
			osw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void apprentissage_mails(int nbSpams,int nbHams) throws Exception {
		apprentissage_mails(nbSpams,nbHams,"./res/baseapp");
	}
	
	public void apprentissage_mails(int nbSpams, int nbHams, String repertoire) throws Exception {
		if (nbSpams <= 0)
			nbSpams = 200;
		if (nbHams <= 0)
			nbHams = 200;
		NSpam = nbSpams;
		NHam = nbHams;
		NMail = NSpam + NHam;
		System.out.println(NMail+" "+NSpam+" "+NHam);
		String SpamDirectory = repertoire+"/spam";
		String HamDirectory = repertoire+"/ham";

		System.out.println("Apprentissage de "+NSpam+" spam et "+NHam+" ham");
		
		int[] apparitionMotsSpam= new int[3];
		int[] apparitionMotsHam = new int[3];
		apparitionMotsSpam = apprentissage_occurrence_mots_mails(SpamDirectory, NSpam);
		//System.out.print(".");
		apparitionMotsHam = apprentissage_occurrence_mots_mails(HamDirectory, NHam); 			//System.out.print(".");
		
		int taille = dictionnaire.size();
		nbSpam = new int[taille];
		nbHam = new int[taille];
		bSpam = new double[taille];
		bHam = new double[taille];
		
		//ESTIMATION DES PARAMETRES AVEC LISSAGE
		for(int i=0; i<taille; i++){
			this.nbSpam[i] = apparitionMotsSpam[i];
			this.nbHam[i] = apparitionMotsHam[i];
			this.bSpam[i] = (double) ( (double) (bSpam[i] + epsilon) / (double) (NSpam + 2*epsilon) );
			this.bHam[i] = (double) ( (double) (bSpam[i] + epsilon) / (double) (NHam + 2*epsilon) );
		}
			
		//CALCULE PROBABILITE A POSTERIORI
		this.PSpam = (double) ( (double) NSpam / (double) NMail) ;
		this.PHam = (double) ( (double) NHam / (double) NMail) ;
		if (PHam + PSpam != 1)	
		{
			System.out.println("\nErreur dans les probabilités Pham +PSpam = "+(PHam+PSpam)+"\n");
			
		}
		System.out.println("PHam : "+PHam+"\t"+"PSpam : "+PSpam);
			
	System.out.println("Apprentissage terminé !");
	}

	public void apprentissage_mail(String mailPath, int type) throws Exception {
		boolean[] vecteur = this.vecteur_occurence(mailPath);
		double probaSpam=0, probaHam=0;

		if (type == 0)
			NHam += 1;
		else
			NSpam += 1;
		for(int i = 0; i < vecteur.length; i++){
			int v = vecteur[i] ? 1 : 0;
			if (type == 0){
				nbHam[i] += v;
				bHam[i] = (nbHam[i]+epsilon)/(NHam+2*epsilon);
			}
			else {
				nbSpam[i] += v;
				bSpam[i] = (nbSpam[i]+epsilon)/(NSpam+2*epsilon);
			}
		}
	}
	
	public int[] apprentissage_occurrence_mots_mails(String directoryName, int nbFichiers) throws Exception {
		int taille = dictionnaire.size();
		int mots[] = new int[taille*10];
		for(int i = 0; i < mots.length;i++){
			mots[i] = 0;
		}
		boolean vecteur[];
		String [] files;
		File repertoire = new File(directoryName);
		files= repertoire.list();
		if(nbFichiers > files.length){
			throw new Exception("Taille base apprentissage choisie trop grande.");
		}
		for(int j=0; j<nbFichiers; j++){
			vecteur = vecteur_occurence(directoryName + "/" + files[j]);
			for(int i = 0; i < vecteur.length;i++){
				if(vecteur[i]){
					mots[i]++;
				}
			}
		}
		return mots;
	}
	
	/*	Fonction qui prend en paramètre l'adresse d'un fichier mail à lire et qui retourne si il a plus de chance d'être un spam ou un ham
	 * 	Retourne 0 si c'est un HAM, 1 si c'est un SPAM, -1 en cas d'erreur.
	 */
	public int evaluer_mail(String mailPath) throws Exception {
		boolean[] vecteur = this.vecteur_occurence(mailPath);
		double probaSpam=0, probaHam=0;
		
		for(int i = 0; i < vecteur.length; i++)
		{
			if (vecteur[i])
			{
				probaSpam += Math.log(bSpam[i]);
				probaHam += Math.log(bHam[i]);
			} else {
				probaSpam += Math.log(1-bSpam[i]);
				probaHam += Math.log(1-bHam[i]);
			}
		}
		if (PSpam > 0)
			probaSpam += Math.log(PSpam);
		if(PHam > 0)
			probaHam += Math.log(PHam);
		
		double px = -Math.log(Math.pow(0.5,dictionnaire.size()));
		probaSpam += px;
		probaHam += px;
		double pHam = 1.0 / (1.0 + Math.exp(probaSpam - probaHam));
		double pSpam = 1.0 / (1.0 + Math.exp(probaHam - probaSpam));
		
		System.out.print(": P(Y=SPAM | X=x) =" + pSpam + ", P(Y=HAM | X=x) =" + pHam+"\n");
		
		if (pSpam > pHam)
			return 1;
		return 0;
	}
	
	public void lancer_tests(int nbSpams, int nbHams) throws Exception {
		lancer_tests(nbSpams, nbHams, "./res/basetest");
	}
	
	public void lancer_tests(int nbSpams, int nbHams, String directoryName) throws Exception {
		int nbTrueSpam = 0, nbSpam, nbTrueHam = 0, nbHam;
		String files[], resultat[] = {"HAM", "SPAM", "ERREUR"};
		//Test sur les SPAMs
		String testDirectoryName = directoryName+"/spam";
		File repertoire = new File(testDirectoryName);
		files= repertoire.list();
		nbSpam = Math.min(files.length,nbSpams);
		int resultatAttendu = 1;
		for (int i = 0; i < nbSpam; i++)
		{
			System.out.print("\n"+resultat[resultatAttendu]+" numero "+i+" ");
			int res = evaluer_mail(testDirectoryName+"/"+files[i]);
			System.out.print("\n          => identifié comme un "+resultat[res]);
			if (res != resultatAttendu){
				nbTrueSpam++;
				System.out.print("  ****ERREUR****");
			}
		}
		//Test sur les HAMs
		testDirectoryName = directoryName+"/ham";
		repertoire = new File(testDirectoryName);
		files= repertoire.list();
		nbHam = Math.min(files.length,nbHams);
		resultatAttendu = 0;
		
		for (int i = 0; i < nbHam; i++)
		{
			System.out.print("\n"+resultat[resultatAttendu]+" numero "+i+" ");
			int res = evaluer_mail(testDirectoryName+"/"+files[i]);
			System.out.print("          => identifié comme un "+resultat[res]);
			if (res != resultatAttendu){
				nbTrueHam++;
				System.out.print("  ****ERREUR****");
			}
		}
		System.out.println("\nErreurs :\nSpam: "+nbTrueSpam+"/"+nbSpam+" => "+(int)(((double)nbTrueSpam/(double)nbSpam)*100)+"%\nHam : "+nbTrueHam+"/"+nbHam+" => "+(int)(((double)nbTrueHam/(double)nbHam)*100)+"%\nGlobal : "+(nbTrueHam+nbTrueSpam)+"/"+(nbSpam+nbHam)+" => "+(int)(((double)(nbTrueHam+nbTrueSpam)/(double)(nbHam+nbSpam))*100)+"\n");
	}
	
	public boolean afficher() {
		System.out.print(NMail+" "+NSpam+" "+NHam);
		for (int i = 0; i < 10; i++){
			System.out.print(" "+bSpam[i]);
		}
		System.out.println("Fini");
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FiltreAntiSpam fas = new FiltreAntiSpam("./res/dictionnaire1000en.txt");
			StringBuilder sb = new StringBuilder();
			sb.append("Nombre d'arguments insuffisants\n");
			sb.append("Pour commencer l'apprentissage :\n      FiltreAntiSpam apprend_filtre [@DossierMailsApprentissage] [NbSpams] [NbHams] [@Classifeur]\n");
			sb.append("Pour continuer l'apprentissage :\n      FiltreAntiSpam apprend_filtre_enligne @Mail TypeMail [@Classifieur] \n");
			sb.append("Pour tester des mails :\n      FiltreAntiSpam filtre_mail [NbSpams] [NbHams] [@Classifieur] [@DossierMailsTest] \n");
			sb.append("Pour tester un mail :\n      FiltreAntiSpam filtre_mail_un @Mail [@Classifieur] \n");
			String repertoire, nom, classifieur;
			int nbSpams = 200, nbHams = 200, type;
			String help = sb.toString();
			if (args.length <= 0){
				System.out.println(help);
			} else 
			{
				switch (args[0])
				{
					case "apprend_filtre":
						if (args.length >= 3)
							nbSpams = Integer.parseInt(args[2]);
						if (args.length >= 4)
							nbHams = Integer.parseInt(args[3]);
						if (args.length < 2)
							fas.apprentissage_mails(nbSpams,nbHams);
						else{
							repertoire = args[1];
							fas.apprentissage_mails(nbSpams,nbHams,repertoire);
						}
						if (args.length < 5)
							fas.sauvegarder_apprentissage();
						else {
							classifieur = args[4];
							fas.sauvegarder_apprentissage(classifieur);
						}
						break;
					case "apprend_filtre_enligne":
						if (args.length < 3)
							System.out.println(help);
						else {
							nom = args[1];
							if (args[2].equals("SPAM"))
								type = 1;
							else if (args[2].equals("HAM"))
								type = 0;
							else{
								System.out.println("Erreur, "+args[2]+" n'est pas un type valide. Seul SPAM et HAM sont des types valides");
								break;
							}
							if (args.length < 4){
								fas.charger_apprentissage();
								fas.apprentissage_mail(nom, type);
								fas.sauvegarder_apprentissage();
								
							}
							else{
								classifieur = args[3];
								fas.charger_apprentissage(classifieur);
								fas.apprentissage_mail(nom, type);
								fas.sauvegarder_apprentissage(classifieur);
							}
						}
						break;
					case "filtre_mail":
						if (args.length >= 2)
							nbSpams = Integer.parseInt(args[1]);
						if (args.length >= 3)
							nbHams = Integer.parseInt(args[2]);
						if (args.length < 4)
							fas.charger_apprentissage();
						else {
							classifieur = args[3];
							fas.charger_apprentissage(classifieur);
						}
						if (args.length < 5)
							fas.lancer_tests(nbSpams, nbHams);
						else{
							repertoire = args[4];
							fas.lancer_tests(nbSpams, nbHams, repertoire);
						}
						break;
					case "filtre_mail_un":
						nom = args[1];
						if (args.length < 3)
							fas.charger_apprentissage();
						else{
							classifieur = args[2];
							fas.charger_apprentissage(classifieur);
						}
						int i = fas.evaluer_mail(nom);
						if (i == 0)
							System.out.println("D'après le classifieur, le message est un HAM.");
						else 
							System.out.println("D'après le classifieur, le message est un SPAM.");
						break;
					case "help":
						System.out.println(help);
						break;
					default:
						System.out.println("Opération inconnue. Pour accéder à l'aide, FiltreAntiSpam help");
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}