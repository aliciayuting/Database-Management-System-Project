package fileIO;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import dataStructure.Tuple;

public class BinaryTupleReader implements TupleReader {
	private String fileName;
	private FileInputStream fin;
	private FileChannel fc;
	private ByteBuffer buffer;
	private int numAttr;
	private int numRows;    // number of rows on current buffer page
	private int curRow;    // keep track of next row to read on the buffer

	public BinaryTupleReader(String fileName) {
		try {
			this.fileName=fileName;
			fin= new FileInputStream(fileName);
			fc= fin.getChannel();
			buffer= ByteBuffer.allocate(4096);
			try {
				fc.read(buffer);
				// Get meta-data
				numAttr= buffer.getInt(0);
				numRows= buffer.getInt(4);
				curRow = 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * read the next tuple
	 * @return the tuple
	 */
	@Override
	public Tuple readNextTuple() {
		if (numRows==0) {
			return null;
		}
		else {
			Integer[] temp= new Integer[numAttr];
			try {
				for (int j= 0; j < numAttr; j++ ) {
					temp[j]= buffer.getInt(curRow * numAttr * 4 + 8 + j * 4);
				}
				curRow+=1;
				// change to next page if current row is the last row
				if(curRow>= numRows) {
					buffer.clear();
					buffer.putInt(4, 0);
					fc.read(buffer);
					numRows= buffer.getInt(4);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return (new Tuple(new ArrayList<Integer>(Arrays.asList(temp))));
		}
	}

	/**
	 *  read whole data of tuples from the file
	 */
	@Override
	public ArrayList<Tuple> readData(){
		ArrayList<Tuple> resource= new ArrayList<Tuple>();
		try {
			while (numRows != 0) {
				for (int i= 0; i < numRows; i+= 1) {
					Integer[] currTuple= new Integer[numAttr];
					for (int j= 0; j < numAttr; j++ ) {
						currTuple[j]= buffer.getInt(i * numAttr * 4 + 8 + j * 4);
					}
					resource.add(new Tuple(new ArrayList<Integer>(Arrays.asList(currTuple))));
				}
				buffer.clear();
				buffer.putInt(4, 0);
				fc.read(buffer);
				numRows= buffer.getInt(4);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resource;
	}

	@Override
	public void close() {
		try {
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset() {
		try {
			fin= new FileInputStream(fileName);

			fc= fin.getChannel();
			buffer= ByteBuffer.allocate(4096);
			fc.read(buffer);
			// Get meta-data
			numAttr= buffer.getInt(0);
			numRows= buffer.getInt(4);
			curRow = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
