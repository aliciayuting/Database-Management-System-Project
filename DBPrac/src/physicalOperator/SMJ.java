package physicalOperator;

import java.util.ArrayList;
import java.util.HashMap;

import dataStructure.Tuple;
import fileIO.TupleWriter;
import net.sf.jsqlparser.expression.Expression;
import parser.EvaluateJoin;

public class SMJ extends Operator {
	private Operator leftOp;
	private Operator rightOp;
	private ArrayList<String> leftColList;
	private ArrayList<String> rightColList;
	private ExternalSortOperator leftExSortOp;
	private ExternalSortOperator rightExSortOp;
	private Tuple tr;
	private Tuple ts;
	private Tuple gs;
	private int ptr;
	boolean flag;

	private boolean ensureEqual(Tuple leftTup, Tuple rightTup, ArrayList<String> leftColList,
		ArrayList<String> rightColList, ArrayList<String> leftSchema, ArrayList<String> rightSchema, int k) {
		for (int i= 0; i < k; i+= 1) {
			if (leftTup.getData(leftSchema.indexOf(leftColList.get(i))) != rightTup
				.getData(rightSchema.indexOf(rightColList.get(i)))) { return false; }
		}
		return true;
	}

	public SMJ(int bufferSize, Operator left, Operator right, Expression joinExpr, HashMap<String, String> alias,
		String dir) {
		EvaluateJoin evalJoin= new EvaluateJoin(joinExpr, left.getTableName(), right.getTableName(), alias);
		leftColList= evalJoin.getJoinAttributesLeft();
		rightColList= evalJoin.getJoinAttributesRight();
		leftOp= left;
		rightOp= right;
		leftExSortOp= new ExternalSortOperator(leftOp, leftColList, bufferSize, dir, "left");
		rightExSortOp= new ExternalSortOperator(rightOp, rightColList, bufferSize, dir, "right");
		tr= leftExSortOp.getNextTuple();
		Tuple firstTuple= rightExSortOp.getNextTuple();
		ts= firstTuple;
		gs= firstTuple;
		ptr= 1;
		flag= false;
	}

	@Override
	public Tuple getNextTuple() {
		while (tr != null && gs != null) {
			if (!flag) {
				int i= 0;
				while (i < leftColList.size()) {
<<<<<<< HEAD
//					if(tr.getData(0)==104 && tr.getData(1)==195) {
//						System.out.println("heiiiii");
//					}
					while (tr!= null &&tr.getTuple().size()>0 && gs != null && tr.getData(leftOp.schema().indexOf(leftColList.get(i))) < gs
=======

					if (tr.getData(0) == 105 && tr.getData(1) == 195) {
						System.out.println("heiiiii");
					}
					while (tr != null && gs != null && tr.getData(leftOp.schema().indexOf(leftColList.get(i))) < gs
>>>>>>> e31b1ba075de8b7b4cf300a7d34381af0277b255
						.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
						tr= leftExSortOp.getNextTuple();
						if (!ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(), i)) {
							i= 0;
							break;
						}
					}
<<<<<<< HEAD
					while (tr != null &&tr.getTuple().size()>0 &&  gs != null &&gs.getTuple().size() > 0  && tr.getData(leftOp.schema().indexOf(leftColList.get(i))) > gs
						.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
						rightExSortOp.resetIndex(ptr);
						gs= rightExSortOp.getNextTuple();
						ptr+= 1;
=======
					while (tr != null && gs != null && gs.getTuple().size() > 0 &&
						tr.getData(leftOp.schema().indexOf(leftColList.get(i))) > gs
							.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
						gs= rightExSortOp.getNextTuple();
						ptr+= 1;
						System.out.println("ptr  " + ptr);
						System.out.println(gs.getData(0));
>>>>>>> e31b1ba075de8b7b4cf300a7d34381af0277b255
						if (!ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(), i)) {
							i= -1;
							break;
						}
					}
					i+= 1;
				}
				rightExSortOp.resetIndex(ptr);
				ts= new Tuple(gs.getTuple());
			}
			if (tr == null || gs == null || tr.getTuple().size() == 0 || gs.getTuple().size() == 0) return null;
			if (ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(),
				leftColList.size())) {
<<<<<<< HEAD
				
				
				
				if (ts != null && ts.getTuple().size() > 0 && ensureEqual(tr, ts, leftColList, rightColList, leftOp.schema(), rightOp.schema(),
					leftColList.size())) {
					
					if(tr.getData(0)==200 && tr.getData(1)==14&&tr.getData(2)==117 && gs.getData(0)==200&& gs.getData(1)==9) {
						System.out.println("goood");
					}
					
=======
				if (ts != null && ts.getTuple().size() > 0 &&
					ensureEqual(tr, ts, leftColList, rightColList, leftOp.schema(), rightOp.schema(),
						leftColList.size())) {
>>>>>>> e31b1ba075de8b7b4cf300a7d34381af0277b255
					flag= true;
					Tuple joinedTuple= new Tuple();
					for (int j= 0; j < leftOp.schema().size(); j++ ) {
						joinedTuple.addData(tr.getData(j));
					}
					for (int j= 0; j < rightOp.schema().size(); j++ ) {
						joinedTuple.addData(ts.getData(j));
					}
					ts= rightExSortOp.getNextTuple();
					ptr+= 1;
					return joinedTuple;
				}
				else {
					flag= false;
					tr= leftExSortOp.getNextTuple();
					rightExSortOp.resetIndex(ptr);
				}
<<<<<<< HEAD
			}else {
=======
			} else {
				System.out.println("=============");
>>>>>>> e31b1ba075de8b7b4cf300a7d34381af0277b255
				flag= false;
				tr= leftExSortOp.getNextTuple();
				rightExSortOp.resetIndex(ptr);
			}
		}
		return null;
	}

	@Override
	public void dump(TupleWriter writer) {
		Tuple t;
		while ((t= getNextTuple()) != null) {
			writer.addNextTuple(t);
		}
		writer.dump();
		writer.close();
	}

	@Override
	public void reset() {
		leftExSortOp.reset();
		rightExSortOp.reset();
		tr= leftExSortOp.getNextTuple();
		Tuple firstTuple= rightExSortOp.getNextTuple();
		ts= firstTuple;
		gs= firstTuple;
	}

	@Override
	public ArrayList<String> schema() {
		leftOp.schema().addAll(rightOp.schema());
		return leftOp.schema();
	}

	@Override
	public String getTableName() {
		return leftOp.getTableName() + "," + rightOp.getTableName();
	}
}
