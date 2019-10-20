package physicalOperator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import dataStructure.DataTable;
import dataStructure.Tuple;
import fileIO.*;

/** the class for the sort operator that sorts the data another operator generates. */
public class SortOperator extends Operator {

	private DataTable buffer;
	private int ptr;
	

	/** @param childOp childOp is the child operator, e.g. ProjectOperator or SelectOperator
	 * @param colList colList is the list of column names to sort data by */
	public SortOperator(Operator childOp, List<String> colList) {
		ptr= -1;
		buffer= new DataTable(childOp.getTableName(), childOp.schema());
		buffer.setFullTable(childOp.getData().getFullTable());
		if (colList == null) {
			buffer.sortData(childOp.schema(), childOp.schema());
		} else {
			buffer.sortData(colList, childOp.schema());
		}
	}

	/** @return the next tuple in the sorted buffer datatable */
	@Override
	public Tuple getNextTuple() {
		ptr+= 1;
		if (ptr < buffer.cardinality()) return new Tuple(buffer.getRow(ptr));
		return null;
	}

	/** @return the schema of the data table that is sorted by the operator */
	@Override
	public ArrayList<String> schema() {
		return buffer.getSchema();
	}

	@Override
	public void reset() {
		ptr= -1;
	}

	@Override
	public void dump(TupleWriter writer) {
		writer.write(buffer.toArrayList());
		writer.close();
	}

	/** @return the name of the table being sorted */
	@Override
	public String getTableName() {
		return buffer.getTableName();
	}

}
