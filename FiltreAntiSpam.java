
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FiltreAntiSpam {
	
	public static final int epsilon = 1;
	
	public double[] bSpam;
	public double[] bHam;
	
	public double PSpam;
	public double PHam;
	
	public int NSpam;
	public int NHam;
	public int NMail;

	private ArrayList<String> dictionnaire;


	public FiltreAntiSpam(String fichier) {
		chargerDictionnaire(fichier);
	}
	
	public void chargerDictionnaire(String fichier){
		dictionnaire = new ArrayList<>();
		String ligne;
		try{
			System.out.println(fichier);
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
		}		
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public boolean[] vecteurOccurence(String fichier){
		boolean mots[] = new boolean[dictionnaire.size()];
		for(int i = 0; i < mots.length;i++){
			mots[i] = false;
		}
		
		String ligne;
		int index;
		String sac[];
		String regex = " ?[,;:...]? | [,;:...]? ?|[,;:...]";
		try{
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
		}		
		catch (Exception e){
			e.printStackTrace();
		}
		
		return mots;
	}
	
	public void chargerApprentissage() {
		try 
		{
			int taille = dictionnaire.size();
			InputStream is=new FileInputStream("./res/mon_classifieur.txt"); 
			InputStreamReader isr=new InputStreamReader(is);
			BufferedReader br=new BufferedReader(isr);
			String line, mots[];
			String regex = "\\|";
			line = br.readLine();
			mots = line.split(regex);
			NMail = Integer.parseInt(mots[0]);
			NSpam = Integer.parseInt(mots[1]);
			NHam = Integer.parseInt(mots[2]);
			bSpam = new double[taille];
			bHam = new double[taille];
			line = br.readLine();
			mots = line.split(regex);
			for (int i = 0; i < taille; i++)
			{
				bSpam[i] = Double.parseDouble(mots[i]);
			}
			line = br.readLine();
			mots = line.split(regex);
			for (int i = 0; i < taille; i++)
			{
				bHam[i] = Double.parseDouble(mots[i]);
			}
			br.close();
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void apprentissage() {
		int taille = dictionnaire.size();
		NSpam = 500;
		NHam = 2500;
		NMail = NSpam + NHam;
		String SpamDirectory = "./res/baseapp/spam";
		String HamDirectory = "./res/baseapp/ham";
		
		System.out.print("Apprentissage");
		
		int[] apparitionMotsSpam= new int[3];
		int[] apparitionMotsHam = new int[3];
		this.bSpam = new double[taille];
		this.bHam = new double[taille];
		try {
			apparitionMotsSpam = apprentissageOccurrenceMotsMail(SpamDirectory, NSpam);
			System.out.print(".");
			apparitionMotsHam = apprentissageOccurrenceMotsMail(HamDirectory, NHam); 
			System.out.print(".");
			
			
			
			//SPAM, estimation des probabilites par les frequences
			for(int i=0; i<taille; i++){
				this.bSpam[i] = (double) ( (double) (apparitionMotsSpam[i] + epsilon) / (double) (NSpam + 2*epsilon) );
			}
			
			//HAM, estimation des probabilites par les frequences
			for(int i=0; i<taille; i++){
				this.bHam[i] = (double) ( (double) (apparitionMotsHam[i] + epsilon) / (double) (NHam + 2*epsilon) );
			}
			
			System.out.print(".");
			
			//SPAM, estimation Probabilite a posteriori P(Y = SPAM)
			this.PSpam = (double) ( (double) NSpam / (double) NMail) ;
			
			//HAM, estimation Probabilite a posteriori P(Y = HAM)
			this.PHam = (double) ( (double) NHam / (double) NMail) ;
			
			System.out.println("PHam : "+PHam+"PSpam : "+PSpam);
			OutputStream os = new FileOutputStream("./res/mon_classifieur.txt");
			OutputStreamWriter osw = new OutputStreamWriter(os);
			osw.write(NMail+"|"+NSpam+"|"+NHam+"|\n");
			StringBuilder sbspam = new StringBuilder(), sbham = new StringBuilder(), sbdic = new StringBuilder();
			for (int i = 0; i < taille; i++){
				String sp = Double.toString(bSpam[i]).substring(0, 10);
				String h = Double.toString(bSpam[i]).substring(0, 10);
				String d = new String(this.dictionnaire.get(i)+"          ").substring(0, 10);
				sbspam.append(sp+"|");
				sbham.append(h+"|");
				sbdic.append(d+"|");
			}
			sbspam.append("\n");
			sbham.append("\n");
			osw.write(sbspam.toString());
			osw.write(sbham.toString());
			osw.write(sbdic.toString());
			osw.close();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public int[] apprentissageOccurrenceMotsMail(String directoryName, int endIndex) throws Exception {
		int mots[] = new int[dictionnaire.size()];
		for(int i = 0; i < mots.length;i++){
			mots[i] = 0;
		}
		boolean vecteur[];
		String [] files;
		File repertoire = new File(directoryName);
		files= repertoire.list();
		System.out.println("nombre de fichiers :"+files.length+"\n");
		if(endIndex > files.length){
			throw new Exception("taille de la base d apprentissage: " + directoryName + " invalide");
		}
		
		for(int j=0; j<endIndex; j++){
			vecteur = vecteurOccurence(directoryName + "/" + files[j]);
			for(int i = 0; i < vecteur.length;i++){
				if(vecteur[i]){
					mots[i]++;
				}
			}
		}

		return mots;
	}
	
	public boolean verifyMail(String path) throws Exception {
		//read file and get binary vector x
		boolean[] x = this.vecteurOccurence(path);
		
		int taille = dictionnaire.size();
		double PMailSpam = 0;
		double PMailHam = 0;
		
		double PXeqx = 500;
		
		boolean j;
		//SPAM
		for(int i=0; i< taille; i++){
			j = x[i];
			if(this.bSpam[i]== 0 |this.bSpam[i]== 1){
				System.out.println("0.0 : "+ this.bSpam[i]);
			}
			if(j == true){
				PMailSpam += this.bSpam[i] > 0 ? Math.log(this.bSpam[i]) : 0;
				PMailSpam = Math.log(this.bSpam[i]) + PMailSpam;
				System.out.println("PMailSpam true: "+PMailSpam);
			} else if(j == false){
				PMailSpam += 1 - this.bSpam[i] > 0 ? Math.log(1 - this.bSpam[i]) : 0;
				PMailSpam = Math.log(1 - this.bSpam[i]) + PMailSpam;
				System.out.println("PMailSpam false: "+PMailSpam);
			} else {
				throw new Exception("Critical Error");
			}
		}
		PMailSpam += this.PSpam > 0 ? Math.log(this.PSpam) : 0;
		PMailSpam = Math.log(this.PSpam) + PMailSpam;
		
		//HAM
		for(int i=0; i<taille; i++){
			j = x[i];
			if(j == true){
				PMailHam += this.bHam[i] > 0 ? Math.log(this.bHam[i]) : 0;
				//PMailHam = Math.log(this.bHam[i]) + PMailHam;
			} else if(j == false){
				PMailHam += 1 - this.bHam[i] > 0 ? Math.log(1 - this.bHam[i]) : 0;
				//PMailHam = Math.log(1 - this.bHam[i]) + PMailHam;
			} else {
				throw new Exception("Critical Error");
			}
		}
		PMailHam += this.PHam > 0 ? Math.log(this.PHam) : 0;
		//PMailHam = Math.log(this.PHam) + PMailHam;
		
		System.out.println(": P(Y=SPAM | X=x) =" + PMailSpam + ", P(Y=HAM | X=x) =" + PMailHam);
		System.out.print("              =>");
		
		// Estimation SPAM ou HAM
		double res = Math.max(PMailSpam, PMailHam);
		if(res == PMailHam){
			// mail considere comme un HAM
			return false;
		} else if(res == PMailSpam){
			// mail considere comme un SPAM
			return true;
		} else {
			throw new Exception("Critical Error");
		}
		//TODO affichage
	}
	
	public void test(String directoryPath) {
		String [] files;
		File repertoire;
		boolean res;
		
		System.out.println();
		System.out.println("Test:");
		System.out.println();
		
		int spamError = 0;
		int hamError = 0;
		int spamSize=0;
		int	hamSize = 0;
		//TEST SPAM
		repertoire = new File(directoryPath + "/spam");
		files= repertoire.list();
		for(String fileName : files){
			spamSize++;
			System.out.print("SPAM " + fileName);
			try {
				res = this.verifyMail(directoryPath + "/spam/" + fileName);
				if(res == true){
					System.out.print("identifie comme un SPAM");
					System.out.println();
				} else {
					System.out.print("identifie comme un HAM  ***Erreur***");
					System.out.println();
					spamError++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//TEST HAM
		repertoire = new File(directoryPath + "/ham");
		files= repertoire.list();
		for(String fileName : files){
			hamSize++;
			System.out.print("HAM " + fileName);
			try {
				res = this.verifyMail(directoryPath + "/ham/" + fileName);
				if(res == true){
					System.out.print(" identifie comme un SPAM  ***Erreur***");
					System.out.println();
					hamError++;
				} else {
					System.out.print(" identifie comme un HAM");
					System.out.println();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(spamError+" erreurs de spam sur "+spamSize+" spams, pourcentage d'erreur : "+ ((float)spamError/(float)spamSize*100) + "%");
		System.out.println(hamError+" erreurs de ham sur "+hamSize+" hams, pourcentage d'erreur : "+ ((float)hamError/(float)hamSize*100) + "%");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FiltreAntiSpam fas = new FiltreAntiSpam("./res/dictionnaire1000en.txt");
		File file = new File("./res/mon_classifieur.txt");
		/*if (!file.exists())
		else*/
		fas.apprentissage();
			//fas.chargerApprentissage();
		//fas.test("./res/basetest");
	}
}