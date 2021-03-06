/**
 * For pass0, we used buffer to sort each table,
 * 	   pass 1 .. end, we use priority to keep track of the first tuple to merge among different runs
 * 		and a hashmap of tupleToReader to keep track of the first tuple to merge and the bufferReader it comes from
 *	The number of tupleReaders represent the number of pages we have in buffer, in order to hold the tuples to sort
 */
package physicalOperator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import dataStructure.Buffer;
import dataStructure.Tuple;
import dataStructure.TupleComparator;
import fileIO.BinaryTupleReader;
import fileIO.BinaryTupleWriter;
import fileIO.ReadableTupleReader;
import fileIO.ReadableTupleWriter;
import fileIO.TupleReader;
import fileIO.TupleWriter;
import utils.PhysicalPlanWriter;

/** the class for the sort operator that sorts the data another operator generates. */
public class ExternalSortOperator extends Operator {

	private PriorityQueue<Tuple> intermediateTable;    // keep track of current
	private Operator childOp;
	private TupleReader sortedReader; 			// keep the sorted result in a file, and access by this reader
	private int totalPass;  					// total number of passes needed
	private Buffer memoryBuffer;
	private int bufferSize;
	private int tuplesBuffer;        			// number of tuples per buffer
	private ArrayList<String> schema;
	private ArrayList<String> colList;
	private int runs;   						// number of files in each pass
	private int pass= 0;   						// the current order of pass
	private String tempDir;
	private String file;
	private String useName;
	private int numAttr;
	private boolean useBinary;

	/** @param childOp childOp is the child operator, e.g. ProjectOperator or SelectOperator
	 * @param colList colList is the list of column names to sort data by */
	public ExternalSortOperator(Operator childOp, List<String> colList, int bufferSize, String tempDir,
		String usageName) {
		this.useBinary= true;
		this.childOp= childOp;
		this.bufferSize= bufferSize;
		this.schema= childOp.schema();
		if(colList==null) {
			colList = new ArrayList<String>();
		}
		this.colList= (ArrayList<String>) colList;
		this.tempDir= tempDir;
		this.useName= usageName;
		this.numAttr= childOp.schema().size();
		tuplesBuffer= (int) Math.floor(1.0 * (4096) * bufferSize / (4.0 * numAttr));
		// tuplesBuffer= this.tuplesPage * bufferSize;
		memoryBuffer= new Buffer(tuplesBuffer);

		// pass0 and get the total number of files stored
		int totalFiles= initialRun();
		if (totalFiles == 0) {
			return;
		}
		
		runs= totalFiles;
		Double div= Math.ceil(1.0 * runs);
		totalPass= (int) Math.ceil(Math.log(div) / Math.log(1.0 * (bufferSize - 1)));

		// pass 1 - totalPass
		for (int curPass= 1; curPass <= totalPass; curPass++ ) {
			int nextRuns= ExternalSort(curPass, runs);
			runs= nextRuns;
		}
		if (useBinary) {
			sortedReader= new BinaryTupleReader(
				tempDir + "/ESInter" + this.useName + Integer.toString(totalPass) + " 0");
		} else {
			sortedReader= new ReadableTupleReader(
				tempDir + "/ESInter" + this.useName + Integer.toString(totalPass) + " 0");
		}
		this.file= tempDir + "/ESInter" + this.useName + Integer.toString(totalPass) + " 0";
	}

	// pass0
	private int initialRun() {
		this.runs= 0;
		Tuple cur;
		while ((cur= childOp.getNextTuple()) != null) {
			if (memoryBuffer.overflow()) {
				memoryBuffer.sortBuffer(colList, schema);
				TupleWriter tuplesWriter;
				if (useBinary) {
					tuplesWriter= new BinaryTupleWriter(
						tempDir + "/ESInter" + this.useName + Integer.toString(pass) + " " + Integer.toString(runs));
				} else {
					tuplesWriter= new ReadableTupleWriter(
						tempDir + "/ESInter" + this.useName + Integer.toString(pass) + " " + Integer.toString(runs));
				}
				// tuplesWriter.write(memoryBuffer.getTuples());
				for (Tuple tup : memoryBuffer.getTuples()) {
					tuplesWriter.addNextTuple(tup);
				}
				tuplesWriter.dump();
				memoryBuffer.clear();
				this.runs++ ;
			}
			memoryBuffer.addData(cur);

		}
		// dump the rest
		if (!(memoryBuffer.empty())) {
			memoryBuffer.sortBuffer(colList, schema);
			TupleWriter tuplesWriter;
			if (useBinary) {
				tuplesWriter= new BinaryTupleWriter(
					tempDir + "/ESInter" + this.useName + Integer.toString(pass) + " " + Integer.toString(runs));
			} else {
				tuplesWriter= new ReadableTupleWriter(
					tempDir + "/ESInter" + this.useName + Integer.toString(pass) + " " + Integer.toString(runs));
			}
			for (Tuple tup : memoryBuffer.getTuples()) {
				tuplesWriter.addNextTuple(tup);
				// tuplesWriter.dump();
			}
			tuplesWriter.dump();
			memoryBuffer.clear();
			this.runs++ ;
		}
		this.pass++ ;
		// the number of runs in current pass
		return this.runs;
	}

	/** @param pathnum the i-th order of pass of this externalSort
	 * @runs runs the number of runs for this pass */
	private int ExternalSort(int passnum, int runs) {
		// the number of merges needed for this pass
		int mergenum= (int) Math.ceil(1.0 * runs / (this.bufferSize - 1));
		int startTable= 0;
		for (int i= 0; i < mergenum; i++ ) {
			int endTable= Math.min(startTable + bufferSize - 1, runs);
			merge(startTable, endTable, i, passnum);
			startTable= endTable;
		}
		this.pass++ ;
		return mergenum;
	}

	/** merge one of the runs from current pass
	 * 
	 * @param runRuns the number of runs in this merge
	 * @param currentRun the order of runs for this merge (currentRun-th run)
	 * @param numMerge the order of merge in current pass */
	private void merge(int firstTable, int endTable, int numMerge, int curPass) {
		HashMap<Tuple, TupleReader> tupleToReader= new HashMap<Tuple, TupleReader>();
		// read previous sorted result and initialization
		intermediateTable= new PriorityQueue(new TupleComparator(this.colList, this.schema));
		for (int i= firstTable; i < endTable; i++ ) {
			TupleReader tupleRead;
			if (useBinary) {
				tupleRead= new BinaryTupleReader(
					tempDir + "/ESInter" + useName + Integer.toString(curPass - 1) + " " + Integer.toString(i));
			} else {
				tupleRead= new ReadableTupleReader(
					tempDir + "/ESInter" + useName + Integer.toString(curPass - 1) + " " + Integer.toString(i));
			}
			Tuple tup;
			tup= tupleRead.readNextTuple();
			intermediateTable.add(tup);
			tupleToReader.put(tup, tupleRead);
		}
		TupleWriter tupleWrite;
		if (useBinary) {
			tupleWrite= new BinaryTupleWriter(
				tempDir + "/ESInter" + useName + Integer.toString(curPass) + " " + Integer.toString(numMerge));
		} else {
			tupleWrite= new ReadableTupleWriter(
				tempDir + "/ESInter" + useName + Integer.toString(curPass) + " " + Integer.toString(numMerge));
		}
		Tuple next;
		Tuple curnext;
		TupleReader curReader;

		// pulling tuple-wise of the first of runs of previous sorted table
		while ((next= intermediateTable.poll()) != null) {
			tupleWrite.addNextTuple(next);
			curReader= tupleToReader.get(next);
			tupleToReader.remove(next);
			if ((curnext= curReader.readNextTuple()) != null) {
				intermediateTable.add(curnext);
				tupleToReader.put(curnext, curReader);

			}
			// if this run finish delete this table from tempdir
			else {
				String dfile= curReader.getFileInfo();
				File deleteFile= new File(dfile);
				if (!deleteFile.delete()) {
					System.out.println("didn't delete this file" + dfile);
				}
			}
		}
		tupleWrite.dump();
	}

	/** @return the next tuple in the sorted TupleReader */
	@Override
	public Tuple getNextTuple() {
		if (sortedReader == null) {
			deleteFile();
			return null;
		}
		
		Tuple tup= sortedReader.readNextTuple();
		if (tup == null) {
			deleteFile();
		}
		return tup;
	}

	/** Delete the intermediate file created by external sort operator */
	public void deleteFile() {
		if (sortedReader == null) {
			return;
		}
		String file= this.file;
		File deleteFile= new File(file);
		deleteFile.delete();
	}

	/** @return the schema of the data table that is sorted by the operator */
	@Override
	public ArrayList<String> schema() {
		return schema;
	}

	@Override
	public void reset() {
		sortedReader.reset();
	}

	/** Reset the next tuple get to index [ind]
	 * 
	 * @param ind The index that will be rewinded back */
	public void resetIndex(int ind) {
		sortedReader.setAtt(schema.size());
		sortedReader.reset(ind);
	}

	@Override
	public void dump(TupleWriter writer) {
		Tuple tup;
		while ((tup= sortedReader.readNextTuple()) != null) {
			writer.addNextTuple(tup);
		}
		writer.dump();
		writer.close();
	}

	/** @return the name of the table being sorted */
	@Override
	public String getTableName() {
		return childOp.getTableName();
	}

	/** Get the file name of the file being read
	 * 
	 * @return the file name */
	public String getFileInfo() {
		return this.file;
	}

	public Operator getChild() {
		return childOp;
	}

	public List<String> getColList() {
		return colList;
	}

	@Override
	public void accept(PhysicalPlanWriter ppw) {
		try {
			ppw.visit(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}