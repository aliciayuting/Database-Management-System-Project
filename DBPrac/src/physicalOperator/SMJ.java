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

	private boolean ensureEqual(Tuple leftTup, Tuple rightTup, ArrayList<String> leftColList,
		ArrayList<String> rightColList, ArrayList<String> leftSchema, ArrayList<String> rightSchema) {
		for (int i= 0; i < leftColList.size(); i+= 1) {
			if (leftTup.getData(leftSchema.indexOf(leftColList.get(i))) != rightTup
				.getData(rightSchema.indexOf(rightColList.get(i)))) { return false; }
		}
		return true;
	}

	public SMJ(int bufferSize, Operator left, Operator right, Expression joinExpr, HashMap<String, String> alias) {
		EvaluateJoin evalJoin= new EvaluateJoin(joinExpr, leftOp.getTableName(), rightOp.getTableName(), alias);
		leftColList= evalJoin.getJoinAttributesLeft();
		rightColList= evalJoin.getJoinAttributesRight();
		leftOp= left;
		rightOp= right;
		leftExSortOp= new ExternalSortOperator(leftOp, leftColList, bufferSize, "/tempdir/");
		rightExSortOp= new ExternalSortOperator(rightOp, rightColList, bufferSize, "/tempdir/");
		tr= leftExSortOp.getNextTuple();
		Tuple firstTuple= rightExSortOp.getNextTuple();
		ts= firstTuple;
		gs= firstTuple;
	}

	@Override
	public Tuple getNextTuple() {
		while (tr != null && gs != null) {
			for (int i= 0; i < leftColList.size(); i++ ) {
				while (tr.getData(leftOp.schema().indexOf(leftColList.get(i))) < gs
					.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
					tr= leftExSortOp.getNextTuple();
				}
				while (tr.getData(leftOp.schema().indexOf(leftColList.get(i))) > gs
					.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
					gs= rightExSortOp.getNextTuple();
				}
			}
			ts= gs;
			while (ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema())) {
				ts= gs;
				while (ensureEqual(tr, ts, leftColList, rightColList, leftOp.schema(), rightOp.schema())) {
					Tuple joinedTuple= tr;
					for (int i= 0; i < rightOp.schema().size(); i++ ) {
						joinedTuple.addData(ts.getData(i));
					}
					ts= rightExSortOp.getNextTuple();
					return joinedTuple;
				}
				gs= ts;
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
