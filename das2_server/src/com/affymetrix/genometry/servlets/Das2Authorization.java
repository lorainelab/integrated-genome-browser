package com.affymetrix.genometry.servlets;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**For restricting access of DAS2 resources to particular users based on the contents of two txt files 
 * placed in the das2 data directory: restrictedDirectories.txt and users.txt*/
public class Das2Authorization {
	//fields
	/**File containing two columns, tab delimited, with a versionedGenomeDirectoryName and a directory within that has restrict access
	 * example: 'S_pombe_Apr_2007	CairnsPrivateData'*/
	private File restrictedDirectoriesFile;

	/**Tab delimited file containing 
	 * 1) userName - unique to file
	 * 2) encryptedPassword - use MD5Crypt.crypt(password, salt) method, see static method DasAuthorization.encrypt (String password)
	 * 3) versionedGenomeDirectory - i.e. 'S_pombe_Apr_2007', should be present in the 'restrictedDirectoriesFile'
	 * 4) restrictedDirectory - directory within the versionedGenomeDirectory granting access
	 * Use multiple lines to grant access for the same user to multiple directories 
	 * example: 'CairnsLab CFfMZEChIYRNdlRtu0JKs0 S_pombe_Apr_2007 CairnsPrivateData'
	 */
	private File usersFile;
	
	/**Salt for md5 encryption.*/
	private static final String md5Salt = "TheGreatSaltLake";
	private ArrayList<String> log = new ArrayList();
	private boolean authorizing = false;
	private Pattern separator = Pattern.compile(File.separator);
	


	/**HashMap of versionedGenome: restrictedDirectory (String), see restrictedDirectoriesFile*/
	HashMap<String,HashSet> restrictedDirectories = null;

	/**HashMap of userName: User (String : User), see userFile*/
	HashMap<String,User> users = null;

	//constructor
	public Das2Authorization(File dataRoot){
		//attempt to initialize
		log.add("Initializing DasAuthorization...");
		
		//does dataRoot exist?
		if (dataRoot.exists()== false) log.add("\tAborting, dataRoot does not exist -> "+dataRoot);
		
		else {
			//create files
			restrictedDirectoriesFile = new File (dataRoot, "restrictedDirectories.txt");
			usersFile = new File (dataRoot, "users.txt");

			//look for required files
			if (usersFile.exists() && restrictedDirectoriesFile.exists()){
				log.add("\tUsersFile: "+usersFile+ "\n"+
						"\tRestrictedDirectoriesFile: "+restrictedDirectoriesFile);
				//load user
				if (loadUserHashMap() == false) log.add("\tAborting, problem loading users.txt file -> "+usersFile);
				//load restrictedDirectories
				else if (loadRestrictedDirectories() == false) log.add("\tAborting, problem loading restrictedDirectories.txt file -> "+restrictedDirectoriesFile);			
				//set to go
				else {
					//check that user directories are restricted
					checkUserDirectories();
					authorizing = true;
					log.add("\tAuthorizing!");
				}
			}
			else log.add("\tMissing '"+usersFile+"' and or '"+restrictedDirectoriesFile+"', aborting.");
		}
		//print log and clear
		printArrayList(log);
		log.clear();
	}




	//methods
	private boolean checkUserDirectories(){
		log.add("\tChecking user restricted directories...");
		boolean allOK = true;
		Iterator it = users.keySet().iterator();
		//for each user
		while (it.hasNext()){
			User user = (User)users.get(it.next());
			//fetch HashMap<versionedGenome, HashSet<protectedDirectories>
			HashMap dirs = user.authorizedDirectories;
			//for each versionedGenome
			Iterator d = dirs.keySet().iterator();
			while (d.hasNext()){
				String versionedGenome = (String)d.next();
				//is versionGenome contain any protected directories
				if (restrictedDirectories.containsKey(versionedGenome) == false){
					log.add("\t\tWARNING: the versionedGenome '"+versionedGenome+"' is not one of the restrictedDirectories! Correct users.txt or restrictedDirectories.txt files");
					allOK = false;
				}
				//yes versionGenome contains some protected directories, fetch and compare
				else {
					//get users protected directories
					HashSet userProtDirs = user.authorizedDirectories.get(versionedGenome);
					//get restricted directories
					HashSet globalProtDirs = restrictedDirectories.get(versionedGenome);
					//for each user directory look to see if it is in the global hash
					Iterator e = userProtDirs.iterator();
					while (e.hasNext()){
						String pd = (String)e.next();
						if (globalProtDirs.contains(pd) == false) {
							log.add("\t\tWARNING: the user specified '"+ pd+"' directory in '"+versionedGenome+"' is not listed in the restrictedDirectories.txt file");
							allOK = false;
						}
					}
				}
			}
		}
		return allOK;
	}

	/**Loads userFile creating users HashMap*/
	private boolean loadUserHashMap(){
		users = new HashMap();
		try{
			BufferedReader in = new BufferedReader(new FileReader(usersFile));
			String line;
			String[] tokens;
			while ((line = in.readLine())!=null){
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) continue;				
				tokens = line.split("\\t+");
				if (tokens.length < 4) continue;
				//does user exist?
				String userName = tokens[0].trim();
				User user;
				if (users.containsKey(userName)) user = (User) users.get(userName);
				else {
					user = new User(tokens[1].trim());
					users.put(userName, user);
				}
				//add new directory?
				HashMap userDirs = user.authorizedDirectories;
				String versionedGenome = tokens[2].trim();
				String protectedDirectory = tokens[3].trim();
				if (userDirs.containsKey(versionedGenome)){
					HashSet dirs = (HashSet)userDirs.get(versionedGenome);
					dirs.add(protectedDirectory);
				}
				else {
					HashSet<String> dirs = new HashSet();
					dirs.add(protectedDirectory);
					userDirs.put(versionedGenome, dirs);
				}
			}
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**Checks if user exists and whether their password matches.
	 * @return - null if not valid or a HashSet of authorized directories.
	 */
	public HashMap validate (String userName, String nonEncryptedPassword){
		if (userName == null || nonEncryptedPassword == null) return null;
		//does user exist
		if (users.containsKey(userName) == false) return null;
		//check password
		String crypted = MD5Crypt.crypt(nonEncryptedPassword, md5Salt);
		User user = (User)users.get(userName);
		if (crypted.equals(user.encryptedPassword)) return user.authorizedDirectories;
		return null;
	} 
	
	/**For encrypting a password using this Classes salt.*/
	public static String encrypt (String password){
		return MD5Crypt.crypt(password, md5Salt);
	}

	/**Loads the restrictedDirectories file into a hash.*/
	private boolean loadRestrictedDirectories(){
		restrictedDirectories = new HashMap();
		try{
			BufferedReader in = new BufferedReader(new FileReader(restrictedDirectoriesFile));
			String line;
			String[] tokens;
			while ((line = in.readLine())!=null){
				line = line.trim();
				if (line.length() ==0 || line.startsWith("#")) continue;
				tokens = line.split("\\t+");
				if (tokens.length < 2) {
					log.add("\tWarning, this restricted directory line doesn't contain two TAB delimited columns! Skipping -> "+line);
					continue;
				}
				//does it already exist?
				if (restrictedDirectories.containsKey(tokens[0])){
					HashSet dirs = restrictedDirectories.get(tokens[0]);
					dirs.add(tokens[1]);
				}
				else {
					HashSet<String> dirs = new HashSet();
					dirs.add(tokens[1]);
					restrictedDirectories.put(tokens[0], dirs);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**Print to System.out the ArrayList*/
	public static void printArrayList(ArrayList al){
		int num = al.size();
		for (int i=0; i< num; i++){
			System.out.println(al.get(i));
		}
	}

	private class User{
		//fields
		String encryptedPassword;
		HashMap<String,HashSet> authorizedDirectories = new HashMap();
		private User (String encryptedPassword) {
			this.encryptedPassword = encryptedPassword;
		}
		public String toString() {
			return authorizedDirectories.toString();
		}
	}

	public HashMap<String,HashSet> getRestrictedDirectories() {
		return restrictedDirectories;
	}
	public boolean isAuthorizing() {
		return authorizing;
	}
	
	/**Looks to see if resource is 1st even restricted and 2nd if resource is contained
	 * in userAccessibleDirectories.*/
	public boolean showResource(HashMap userAccessibleDirectories, String versionedGenomeDirectory, String requestedResource){
		//does the versionedGenomeDirectory contain any restrictedDirectories?
		if (restrictedDirectories.containsKey(versionedGenomeDirectory) == false) return true;
		
		//is it a restricted directory
		//get global restricted directories under versionedGenome
		HashSet restrictedDirs = restrictedDirectories.get(versionedGenomeDirectory);
		//split requestedResource by the file separator
		String[] t = separator.split(requestedResource);
		if (restrictedDirs.contains(t[0]) == false) return true;
		
		//OK, it's restricted, can they view it?
		//do they have any permitted directories?
		if (userAccessibleDirectories == null) return false;
		//get user dirs
		HashSet userPermittedDirs = (HashSet)userAccessibleDirectories.get(versionedGenomeDirectory);

		if (userPermittedDirs != null && userPermittedDirs.contains(t[0])) return true;
		return false;
	}

	//for testing
	/*public static void main (String[] args){
		DasAuthorization da = new DasAuthorization(new File ("/Users/nix/HCI/DAS/Das2Data/"));
		System.out.println("RestrictedDirectories "+da.getRestrictedDirectories());
		System.out.println("Users "+da.users);
		HashMap rDirs = da.validate("GravesLab", "rat");
		System.out.println("Validated user? "+rDirs);
		if (da.isAuthorizing()){
			boolean showIt = da.showResource(rDirs, "S_pombe_Apr_2007","Cairns/TotalS");
			System.out.println("ShowIt? "+showIt);
		}
	}*/
}
