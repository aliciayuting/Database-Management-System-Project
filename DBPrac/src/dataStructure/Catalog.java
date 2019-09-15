package dataStructure;

import java.util.ArrayList;
import java.util.HashMap;

public class Catalog {
	
	private static Catalog dbCatalog = null;
	private HashMap<String, String> tableDir;
	private HashMap<String, ArrayList<String>> schemaList;
	
	private Catalog() {
		tableDir = new HashMap<String, String>();
		schemaList = new HashMap<String, ArrayList<String>>();
	}
	
	public static Catalog getInstance() {
		if (dbCatalog == null) {
			dbCatalog = new Catalog();
		}
		return dbCatalog;
	}
	
	public String getDir(String name) {
		return tableDir.get(name);
	}
	
	public void addDir(String name, String dir) {
		tableDir.put(name, dir);
	}
	
	public void addSchema(String name, ArrayList<String> schema) {
		schemaList.put(name, schema);
	}

	public ArrayList<String> getSchema(String name) {
		return schemaList.get(name);
	}
	
	public void printCatalog() {
		System.out.println("Tables directorys:");
		for(String table: tableDir.keySet()) {
			System.out.println(table+tableDir.get(table));
		}
	}
}
