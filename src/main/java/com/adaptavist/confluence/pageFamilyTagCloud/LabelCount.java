package com.adaptavist.confluence.pageFamilyTagCloud;

import java.util.Comparator;

import com.atlassian.confluence.labels.Label;

public class LabelCount {

	protected Label label;
	protected int count;
	protected String link;
	

	public LabelCount(Label label, int count){
		this.label = label;
		this.count = count;
	}

	public String getLink() {
		return link;
	}
	
	public void setLink(String link) {
		this.link = link;
	}

	public Label getLabel() {
		return label;
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	
	static protected class LabelCountLabelComparator implements Comparator{
		public int compare(Object arg0, Object arg1) {
			return ((LabelCount)arg0).label.compareTo(((LabelCount)arg1).label);
		}
	}
	
	static protected class LabelCountCountComparator implements Comparator{
		public int compare(Object arg0, Object arg1) {
			
			LabelCount labelCount0 = (LabelCount)arg0;
			LabelCount labelCount1 = (LabelCount)arg1;
			
			int result = labelCount0.count - labelCount1.count;
			if(result == 0){
				return labelCount0.label.compareTo(labelCount1.label);
			}
			return result;
		}
	} 
	
	public static final Comparator LABEL_COMPARATOR = new LabelCountLabelComparator();
	public static final Comparator COUNT_COMPARATOR = new LabelCountCountComparator();
	
	
}
