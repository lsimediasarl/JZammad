package ch.bavitech.zammad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ZammadRequest {
	protected int offset = 1;
	protected int limit = 100;
	
	protected String sort_by = "";
	protected String order_by = "";
	
	protected String signature = "";
	protected String endPoint = "";
	protected String userData = "";
	
	HashMap<String, Object> fields = new HashMap<>();
	
	public ZammadRequest(String signature, String endPoint, String userData) {
		this.endPoint = endPoint;
		this.signature = signature;
		this.userData = userData;

	}

	//***************************************************************************
	//*** API
	//***************************************************************************
	public void setLimit(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}
	
	/**
	 * order_by is "asc" or "desc", the sort_by is the column name
	 * 
	 * Only use in search end points
	 * 
	 * @param sort_by
	 * @param order_by 
	 */
	public void setSortBy(String sort_by, String order_by) {
		this.sort_by = sort_by;
		this.order_by = order_by;
		
	}
	
	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}
	
	public String getSortBy() {
		return sort_by;
	}
	
	public String getOrderBy() {
		return order_by;
		
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void setField(String field, Object value) {
		fields.put(field, value);
	}
	
	public String getSignature() {
		return signature;
	}
	
	public String getUserData() {
		return userData;
	}
	
	/**
	 * Return the json for the request
	 * @return 
	 */
	@Override
	public String toString() {
		if (fields.isEmpty()) return "";
		
		StringBuilder b = new StringBuilder();
		b.append("{\n");
		Iterator<String> it = fields.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			Object obj = fields.get(name);
			if (obj instanceof String) {
				b.append("\""+name+"\":"+obj.toString());
				
			} else if (obj instanceof ZammadEntry) {
				ZammadEntry entry = (ZammadEntry) obj;
				b.append("\""+name+"\":"+entry.toString());
				
			} else if (obj instanceof ArrayList) {
				ArrayList<Object> list = (ArrayList<Object>) obj;
				b.append("\""+name+"\":");
				b.append("[");
				for (int i=0;i<list.size();i++) {
					b.append(list.get(i).toString());
					if (i<list.size()-1) b.append(",");
				}
				b.append("]");
			}
			if (it.hasNext()) b.append(",");
		}
		b.append("}\n");
		return b.toString();
		
	}
}
