package ch.lsimedia.zammad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author sbodmer
 */
public class ZammadEntry {

	HashMap<String, Object> fields = new HashMap<>();

	public ZammadEntry() {
		//---
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		Iterator<String> it = fields.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			Object obj = fields.get(field);
			if (obj instanceof ZammadEntry) {
				ZammadEntry entry = (ZammadEntry) obj;
				b.append(field+" = [Entry] "+entry.toString()+"\n");
				
			} else if (obj instanceof String) {
				b.append(field+" = "+obj.toString()+"\n");
				
			} else if (obj instanceof ArrayList) {
				ArrayList<Object> list = (ArrayList<Object>) obj;	
				b.append(field+" = [Array]\n");
				for (int i=0;i<list.size();i++) {
					b.append("["+i+"] = "+list.get(i).toString()+"\n");
				}
			}
		
		}
		return b.toString();
	}
	
	//***************************************************************************
	//*** API
	//***************************************************************************
	public void setField(String field, Object value) {
		fields.put(field, value);
	}
	
	/**
	 * Could return
	 * - String
	 * - ZammadEntry
	 * - Array<Object>
	 *
	 * If field is not found return empty string
	 * @param field
	 * @return 
	 */
	public Object getField(String field) {
		return fields.get(field);
	}
	
	/**
	 * Return original map
	 * 
	 * @return 
	 */
	public HashMap<String, Object> getFields() {
		return fields;
	}
}
