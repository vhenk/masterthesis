package scigraph_dataset;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class GetAuthorConferences {
	static class Author {
		String id, name; 
		public Author(String id, String name) {
			this.id = id;	this.name = name;
		}
	}
	
	private static ArrayList<Author> getAuthors(String path) {
		ArrayList<Author> authors = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1)	
					authors.add(new Author(line.substring(0, line.indexOf("\t")), line.substring(line.indexOf("\t")+1).trim()));
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		return authors;
	}
	
	private static void filterContributions(ArrayList<Author> authors, String cpath, String outputpath) {
		try(BufferedReader br = new BufferedReader(new FileReader(cpath))) {
			String line;
			while((line = br.readLine()) != null) {
				int pos1 = line.indexOf("publishedName>");	String name = "";	String cID = "";
				if(pos1 != -1) {
					name = line.substring(pos1 + 16, line.length()-3);
					String id = containsName(authors, name);
					if(!id.equals("")) {
						int pos2 = line.indexOf("/things/contributions/");
						cID = line.substring(pos2 + 22, line.indexOf(">"));
						appendToFile(outputpath, id + "\t" + cID + "\n");
					}
				}
			}br.close();
		} catch(IOException e) {	e.printStackTrace();	}
	}
	
	private static String containsName(ArrayList<Author> list, String name) {
		String id = "";
		for(Author a : list)	
			if(a.name.equals(name))	return a.id;
		return id;
	}
	
	private static boolean containsItem(ArrayList<String[]> list, String[] item) {
		boolean exists = false;
		for(String[] i : list) {
			if((i[0].equals(item[0])) && (i[1].equals(item[1]))) {	exists = true; break;	}
		}
		return exists;
	}
	
	private static ArrayList<String> getAuthID(ArrayList<String[]> clist, String contribution) {
		ArrayList<String> ids = new ArrayList<>();
		for(String[] c : clist) 	if(c[1].equals(contribution))	ids.add(c[0]);
		return ids;
	}
	
	private static void convertCont2Books(String auth_cont_path, String sgcpath, String sgbpath, String outputpath1, String outputpath2) {
		// Retrieve Author-IDs and corresponding Contributions:
		ArrayList<String[]> clist = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(auth_cont_path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String[] a = new String[2];
					a[0] = line.substring(0, line.indexOf("\t"));	a[1] = line.substring(line.indexOf("\t")+1);
					clist.add(a);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		System.out.println("List with " + clist.size() + " entries was created!");
		
		// Retrieve Publication-IDs for Contributions in clist from sgcpath and store them in outputpath1:
		System.out.println("Retrieving Publications for Contribution-IDs ....");
		ArrayList<String[]> plist = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(sgcpath))) {
			String line;
			while((line = br.readLine()) != null) {
				int pos = line.indexOf("/contributions/");
				if(pos != -1) {
					String ctmp = line.substring(pos + 15, line.length() - 3);
					ArrayList<String> idstmp = getAuthID(clist, ctmp);
					if(idstmp.size() != 0) {
						String publication = line.substring(line.indexOf("things/book-chapters/")+21, line.indexOf(">"));
						for(String id : idstmp) {
							plist.add(new String[] {id, publication});
							appendToFile(outputpath1, id + "\t" + publication + "\n");
						}
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Retrieve Book-IDs for Publications from sgbpath and store them in outputpath2:
		System.out.println("Retrieving Books for Publication-IDs ....");
		try(BufferedReader br = new BufferedReader(new FileReader(sgbpath))) {
			String line;
			while((line = br.readLine()) != null) {
				int pos = line.indexOf("/book-chapters/");
				if(pos != -1) {
					String ptmp = line.substring(pos + 15, line.indexOf(">"));
					ArrayList<String> idstmp = getAuthID(plist, ptmp);
					if(idstmp.size() != 0) {
						String book = line.substring(line.indexOf("/things/books/") + 14, line.length()-3);
						for(String id : idstmp)	appendToFile(outputpath2, id + "\t" + book + "\n");
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// <http://scigraph.springernature.com/things/book-chapters/fafbfb3d7e3027a87a9837d68464b682> 
		//<http://scigraph.springernature.com/ontologies/core/hasContribution> 
		//<http://scigraph.springernature.com/things/contributions/aad0d8cfc2307b2f76f0fa6ef583cbf3> .
				
		// <http://scigraph.springernature.com/things/book-chapters/fafbfb3d7e3027a87a9837d68464b682> 
		//<http://scigraph.springernature.com/ontologies/core/hasBook> 
		//<http://scigraph.springernature.com/things/books/a48649aa9fbcb6bdd8c9861ea18d4071> .
	}
	
	private static void convertBooks2ConfNames(String auth_book_path, String sgbookspath, String sgconfpath, String outputpath) {
		// Retrieve Author-IDs and corresponding Books:
		ArrayList<String[]> blist = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(auth_book_path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String[] a = new String[2];
					a[0] = line.substring(0, line.indexOf("\t"));	a[1] = line.substring(line.indexOf("\t")+1);
					blist.add(a);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
				
		System.out.println("List with " + blist.size() + " entries was created!");
		
		// Retrieve Conference-IDs for Books in blist from sgbookspath and store them in clist:
		System.out.println("Retrieving Conference-IDs for Book-IDs ....");		
		ArrayList<String[]>	clist = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(sgbookspath))) {
			String line;
			while((line = br.readLine()) != null) {
				int pos = line.indexOf("/things/books/");
				if(pos != -1) {
					String btmp = line.substring(pos + 14, line.indexOf(">"));
					ArrayList<String> idstmp = getAuthID(blist, btmp);
					if(idstmp.size() != 0) {
						String conference = line.substring(line.indexOf("things/conferences/") + 19, line.length() - 3);
						for(String id : idstmp)	clist.add(new String[] {id, conference});		
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// Retrieve Conference-Acronyms for Coference-IDs from sgconfpath and store them in outputpath:
		System.out.println("Retrieving Acronyms of Conferences ....");
		try(BufferedReader br = new BufferedReader(new FileReader(sgconfpath))) {
			String line;
			while((line = br.readLine()) != null) {
				int pos = line.indexOf("/things/conferences");
				if(pos != -1) {
					String ctmp = line.substring(pos + 20, line.indexOf(">"));
					ArrayList<String> idstmp = getAuthID(clist, ctmp);
					if(idstmp.size() != 0) {
						String acronym = line.substring(line.indexOf("/core/acronym") + 16, line.length() - 3);
						for(String id : idstmp) appendToFile(outputpath, id + "\t" + acronym + "\n");
					}
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		// <http://scigraph.springernature.com/things/books/d3fd1e038b7c3a34a368b52fe5a52692> 
		// <http://scigraph.springernature.com/ontologies/core/hasConference>
		// <http://scigraph.springernature.com/things/conferences/17f7811fe86bab9d3f30ac661974a8d3> .
		
		// <http://scigraph.springernature.com/things/conferences/a84d70a229675782e39f3b8d16a8008e>
		// <http://scigraph.springernature.com/ontologies/core/acronym> "ADBIS" .		
	}
	
	private static void removeDuplicates(String path, String newpath) {
		ArrayList<String[]> list = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.indexOf("\t") != -1) {
					String[] item = new String[2];
					item[0] = line.substring(0, line.indexOf("\t"));	item[1] = line.substring(line.indexOf("\t")+1);
					list.add(item);
				}
			} br.close();
		} catch(IOException e) {	e.printStackTrace();	}
		
		ArrayList<String[]> newlist = new ArrayList<>();
		for(String[] l : list) 	if(!containsItem(newlist, l))	newlist.add(l);
		for(String[] l : newlist)	appendToFile(newpath, l[0] + "\t" + l[1] + "\n");
	}
	
	private static void appendToFile(String path, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			fos.write(content.getBytes());	fos.close();
		} catch(IOException e)	{	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String tmppath = "data_sg/tmp";	String fpath = "data_sg/data_filtered";
		
		File tmpdir = new File(tmppath);	tmpdir.mkdir();
		String author_cont_path = tmppath + "/author_cont_all.txt";		String author_book_path = tmppath + "/author_books_all.txt";	
		String author_b_filtered_path = tmppath + "/author_books_all_filtered.txt";
		
		String contpath = fpath + "/sg_contributors_filtered.nt";	String cont2path = fpath + "/sgcontributions.nt";
		String bookspath = fpath + "/sgbooks.nt";	String booksfpath = fpath + "/sg_bookdata_filtered.nt";
		String confpath = fpath + "/sg_conferencedata_filtered.nt";
		
		String authpath = "data_sg/output/author-key-map.txt";	String authorconfspath = "data_sg/output/author_confs.txt";
		String author_publ_path = "data_sg/output/author_publ_all.txt";
		
		ArrayList<Author> authors = getAuthors(authpath);
		filterContributions(authors, contpath, author_cont_path);
		convertCont2Books(author_cont_path, cont2path, bookspath, author_publ_path, author_book_path);
		removeDuplicates(author_book_path, author_b_filtered_path);
		convertBooks2ConfNames(author_b_filtered_path, booksfpath, confpath, authorconfspath); 
		
		System.out.println("Removing temporary data ....");
		File tmp1 = new File(author_cont_path);	tmp1.delete();
		File tmp2 = new File(author_book_path);	tmp2.delete();
		File tmp3 = new File(author_b_filtered_path);	tmp3.delete();
		tmpdir.delete();
	}
	
}
