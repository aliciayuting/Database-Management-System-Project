package dataStructure;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataTable {

	private String name;
	private ArrayList<ArrayList<Integer>> data;
	private ArrayList<String> schema;

	public DataTable(String tableName, ArrayList<String> schema) {
		name= tableName;
		data= new ArrayList<ArrayList<Integer>>();
		this.schema= schema;
	}

	/**
	 * 
	 * @return the name of the table
	 */
	public String getTableName() {
		return name;
	}
	
	/**
	 * 
	 * @return the full data Table's data
	 */
	public ArrayList<ArrayList<Integer>> getFullTable(){
		return ((ArrayList<ArrayList<Integer>>) data.clone());
	}
	
	/**
	 * 
	 * @param d set the full datatable's data to d
	 */
	public void setFullTable(ArrayList<ArrayList<Integer>> d) {
		this.data=d;
	}
		

	public ArrayList<String> getSchema() {
		return schema;
	}

	public void setSchema(ArrayList<String> changeSchema) {
		this.schema= changeSchema;
	}

	public void addData(ArrayList<Integer> newData) {
		data.add(newData);
	}

	public void addData(Tuple tup) {
		data.add(tup.getTuple());
	}

	public int cardinality() {
		return data.size();
	}

	public ArrayList<Integer> getData(String columnName) {
		return data.get(schema.indexOf(columnName));
	}

	public ArrayList<Integer> getRow(int r) {
		return data.get(r);
	}

	public void sortData(List<String> colList, ArrayList<String> colSchema) {
		Comparator<ArrayList<Integer>> myComparator= new Comparator<ArrayList<Integer>>() {
			@Override
			public int compare(ArrayList<Integer> arr1, ArrayList<Integer> arr2) {
				int result= 0;
				int ptr= 0;
//				System.out.println(colList);
				while (ptr < colList.size() && result == 0) {
					result= arr1.get(colSchema.indexOf(colList.get(ptr))) -
						arr2.get(colSchema.indexOf(colList.get(ptr)));
					ptr+= 1;
				}
				return result;
			}
		};
//		System.out.println("col lst is " + colList);
		data.sort(myComparator);
//		data.sort((l1, l2) -> l1.get(colIndex).compareTo(l2.get(colIndex)));
	}

	/**
	 * 
	 * @param ps
	 */
	public void printTable(PrintStream ps) {
		for (ArrayList<Integer> x : data) {
			for (int y : x) {
				ps.print(y + " ");
			}
			ps.println();
		}
	}

	public void printTableInfo() {
		System.out.println("Table name: " + name);

		Catalog catalog= Catalog.getInstance();
		System.out.println("Table directory: " + catalog.getDir(name));

		System.out.print("Table schema is: [");
		for (String columnName : schema) {
			System.out.print(columnName + " ");
		}
		System.out.print("]\n");
	}
}
