package fileIO;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import dataStructure.Tuple;

public class BinaryTupleReader implements TupleReader {
	private String file;          // the directory + file name
	private FileInputStream fin;
	private FileChannel fc;
	private ByteBuffer buffer;
	private int numAttr;
	private int numRows;    // number of rows on current buffer page
	private int curRow;    // keep track of next row to read on the buffer
	private ArrayList<Tuple> pageData;
	private int rowsPerPage;
	private int pageNum;

	/** Binary tuple Reader consturctor
	 * 
	 * @param file the path of the file */
	public BinaryTupleReader(String file) {
		try {
			this.file= file;
			this.pageData= new ArrayList<Tuple>();
			fin= new FileInputStream(file);
			fc= fin.getChannel();
			buffer= ByteBuffer.allocate(4096);
			curRow= 0;
			numRows= 0;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		pageNum= 0;
		pageData= getNextPage();
	}

	/** Return in the next page of data
	 * 
	 * @return an arraylist of tuples that can be read in a page */
	private ArrayList<Tuple> getNextPage() {
		buffer.clear();
		buffer.putInt(4, 0);

		try {
			fc.read(buffer);
			this.numAttr= buffer.getInt(0);
			this.numRows= buffer.getInt(4);
			if (this.numRows != 0) {
				this.rowsPerPage= (int) Math.floor(1.0 * (4096 - 8) / (this.numAttr * 4));
				pageData= new ArrayList<Tuple>();
				// Populate the dataTable
				for (int i= 0; i < this.numRows; i++ ) {
					ArrayList<Integer> temp= new ArrayList<Integer>(numAttr);
					for (int j= 0; j < numAttr; j++ ) {
						temp.add(buffer.getInt(i * numAttr * 4 + 8 + j * 4));
					}
					pageData.add(new Tuple(temp));
				}
				pageNum++ ;
				return pageData;
			}
			this.pageData= null;
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error when reading into the buffer");
		}
		return null;
	}

	@Override
	public Tuple readNextTuple() {
		if (curRow <= this.numRows - 1) {
			return pageData.get(curRow++);
		} else {
			curRow= curRow - numRows;
			pageData= getNextPage();
			if (pageData == null || curRow >= this.numRows) { return null; }

			if (this.numRows > 0) {
				return pageData.get(curRow++);
			} else {
				return null;
			}
		}
	}

	@Override
	public void setAtt(int num) {
		this.numAttr= num;
	}

	@Override
	public String getFileInfo() {
		return this.file;
	}

	@Override
	public void close() {
		try {
			fc.close();
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int[] getTupleLoc() {
		if (curRow == 0) {
			return new int[] { pageNum - 1, numRows - 1 };
		} else {
			return new int[] { pageNum - 1, curRow - 1 };
		}
	}

	@Override
	public void reset() {
		try {
			fin= new FileInputStream(file);
			fc= fin.getChannel();
			buffer= ByteBuffer.allocate(4096);
			curRow= 0;
			pageNum= 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
		pageData= getNextPage();
	}

	@Override
	public void reset(int index) {
		// Using position(long newPosition) method in FileChannel
		try {
			int pageIndex= index / this.rowsPerPage;
			pageNum= pageIndex;
			curRow= index % rowsPerPage;
			fc.position(pageIndex * 4096);
			this.pageData= getNextPage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int pageInd, int rowInd) {

		// Using position(long newPosition) method in FileChannel
		try {
			pageNum= pageInd;
			curRow= rowInd;
			fc.position(pageInd * 4096);
			this.pageData= getNextPage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}