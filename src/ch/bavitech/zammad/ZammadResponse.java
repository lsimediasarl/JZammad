package ch.bavitech.zammad;

import java.util.ArrayList;
import java.util.Date;

/**
 * Simple JSON parser
 *
 * @author sbodmer
 */
public class ZammadResponse extends Object {

	protected String signature = "";
	protected String userData = "";

	protected ArrayList<ZammadSys> syss = new ArrayList<>();
	protected ArrayList<Object> entries = new ArrayList<>();

	/**
	 * Create a new KnopMessage with a hashcode for the signature
	 */
	public ZammadResponse() {
		this("" + (new Date()).hashCode() + "");
	}

	public ZammadResponse(String signature) {
		this.signature = signature;
	}

	/**
	 * Parse the passed json to internal objects, could be a search response
	 * which have a different syntaxe (grr...)
	 */
	public ZammadResponse(String signature, String json) {
		this.signature = signature;

		Index in = new Index();
		//--- Parser the json
		if (json.startsWith("[")) {
			//--- Multiple entries in direct tickets list context
			ArrayList<Object> list = parseArrays(json, in);
			for (int i = 0;i < list.size();i++) entries.add(list.get(i));

		} else if (json.startsWith("{\"tickets\"")) {
			//--- Parse to find the list of tickets
			// {"tickets":[107,106,105,104,103,102,101,100,99,98,97,96,95,94,93,92,91,90,89,88,87,86,85,84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69,68,67,66,65,64,63,62,61,60,59,58,57,56,55,53,52,51,50,49,48,47,46,45,44,43,42,41,40,39,38,37,36,35,34,33,32,31,30,29,28,27,25,24,23,22,21,20,18,17,16,15,14,13,12,11,10,9,8,7,6,5],"tickets_count":100,"assets":
			ArrayList<ZammadEntry> list = parseSearchResult(json, in);
			for (int i=0;i<list.size();i++) entries.add(list.get(i));
			
		} else if (json.startsWith("{")) {

			//--- Found unique entry or search response
			ZammadEntry entry = parseEntry(json, in);
			entries.add(entry);
		}

	}

	//***************************************************************************
	//*** API
	//***************************************************************************
	/**
	 * Clear everything, except the signature
	 */
	public void clear() {
		syss.clear();
		entries.clear();
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}

	public void setUserData(String userData) {
		this.userData = userData;
	}

	public String getUserData() {
		return userData;
	}

	public void add(ZammadSys sys) {
		syss.add(sys);

	}

	public void add(ZammadEntry entry) {
		entries.add(entry);

	}

	/**
	 * Return the original array of the entries
	 *
	 * @return
	 */
	public ArrayList<Object> getEntries() {
		return entries;
	}

	/**
	 * Return first entry or null if none
	 *
	 * @return
	 */
	public Object getEntry() {
		if (entries.isEmpty()) return null;
		return entries.get(0);
	}

	/**
	 * Return original array of the sys messages
	 *
	 * @return
	 */
	public ArrayList<ZammadSys> getSyss() {
		return syss;
	}

	/**
	 * Return first sys or null if none
	 *
	 * @return
	 */
	public ZammadSys getSys() {
		if (syss.isEmpty()) return null;
		return syss.get(0);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int i = 0;i < syss.size();i++) s.append(syss.get(i).toString());
		for (int i = 0;i < entries.size();i++) s.append(entries.get(i).toString());

		return s.toString();
	}

	//**************************************************************************
	//*** Private
	//**************************************************************************
	/**
	 * The passed json starts with [, parse until ] is found
	 *
	 * [1,2,3]
	 *
	 * @param json
	 * @param in
	 */
	private ArrayList<Object> parseArrays(String json, Index in) {
		// System.out.println(">>> PARSE ARRAY");
		ArrayList<Object> list = new ArrayList<>();
		in.index++; //--- Skip first [
		while (in.index < json.length()) {
			char c = json.charAt(in.index);
			if (c == '{') {
				//--- Found entry, parse it
				ZammadEntry entry = parseEntry(json, in);
				list.add(entry);

			} else if (c == ']') {
				in.index++;
				break;

			} else if (c == ',') {
				in.index++;	//--- Skip it, so next entry can be processed

			} else if (c == '"') {
				String value = parseString(json, in);
				list.add(value);

			} else {
				String value = parseNonString(json, in);
				list.add(value);

			}

		}
		// System.out.println("<<< END PARSE ARRAY");
		return list;
	}

	public ArrayList<ZammadEntry> parseSearchResult(String json, Index in) {
		ArrayList<ZammadEntry> list = new ArrayList<>();
		// {"tickets":[107,106,105,104,103,102,101,100,99,98,97,96,95,94,93,92,91,90,89,88,87,86,85,84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69,68,67,66,65,64,63,62,61,60,59,58,57,56,55,53,52,51,50,49,48,47,46,45,44,43,42,41,40,39,38,37,36,35,34,33,32,31,30,29,28,27,25,24,23,22,21,20,18,17,16,15,14,13,12,11,10,9,8,7,6,5],"tickets_count":100,"assets":
		in.increment();	//--- skip first {
		//--- Read until last } is reached
		while (in.index < json.length()) {
			char c = json.charAt(in.index);
			if (c == '}') {
				in.increment();
				break;

			} else if (c == ',') {
				//--- Read next field
				in.increment();
				continue;
			}

			//--- Find field
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;
			in.index = index + 1;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			if (name.equals("tickets")) {
				c = json.charAt(in.index);
				if (c == '[') {
					//--- Read until ]
					index = json.indexOf(']', in.index);
					if (index == -1) break;
					in.index = index + 1;
				}

			} else if (name.equals("tickets_count")) {
				//--- Read value
				index = json.indexOf(',', in.index);
				in.index = index + 1;

			} else if (name.equals("assets")) {
				in.increment();	//--- Skip first {
				while (in.index < json.length()) {
					c = json.charAt(in.index);
					if (c == '}') {
						in.increment();
						break;

					} else if (c == ',') {
						//--- Read next field
						in.increment();
						continue;
					}

					begin = in.index;
					index = json.indexOf(':', in.index);
					if (index == -1) break;
					in.index = index + 1;

					name = json.substring(begin, index).trim();
					name = name.replaceAll("\"", "");
					if (name.equals("Ticket")) {
						in.increment(); //--- Skip first {
						while (in.index < json.length()) {
							c = json.charAt(in.index);
							if (c == '}') {
								in.increment();
								break;

							} else if (c == ',') {
								//--- Read next field
								in.increment();
								continue;
							}

							begin = in.index;
							index = json.indexOf(':', in.index);
							if (index == -1) break;
							in.index = index + 1;

							//--- Ticket id
							name = json.substring(begin, index).trim();
							name = name.replaceAll("\"", "");
							
							ZammadEntry entry = parseEntry(json, in);
							list.add(entry);

						}

					} else if (name.equals("User")) {
						in.increment(); //--- Skip first {
						while (in.index < json.length()) {
							c = json.charAt(in.index);
							if (c == '}') {
								in.increment();
								break;

							} else if (c == ',') {
								//--- Read next field
								in.increment();
								continue;
							}

							begin = in.index;
							index = json.indexOf(':', in.index);
							if (index == -1) break;
							in.index = index + 1;

							//--- User id
							name = json.substring(begin, index).trim();
							name = name.replaceAll("\"", "");
							
							ZammadEntry entry = parseUser(json, in);
							list.add(entry);

						}
						
					} else if (name.equals("Role")) {
						in.increment(); //--- Skip first {
						while (in.index < json.length()) {
							c = json.charAt(in.index);
							if (c == '}') {
								in.increment();
								break;

							} else if (c == ',') {
								//--- Read next field
								in.increment();
								continue;
							}

							begin = in.index;
							index = json.indexOf(':', in.index);
							if (index == -1) break;
							in.index = index + 1;

							//--- User id
							name = json.substring(begin, index).trim();
							name = name.replaceAll("\"", "");
							
							ZammadEntry entry = parseRole(json, in);
							list.add(entry);

						}
						
					} else if (name.equals("Organization")) {
						in.increment(); //--- Skip first {
						while (in.index < json.length()) {
							c = json.charAt(in.index);
							if (c == '}') {
								in.increment();
								break;

							} else if (c == ',') {
								//--- Read next field
								in.increment();
								continue;
							}

							begin = in.index;
							index = json.indexOf(':', in.index);
							if (index == -1) break;
							in.index = index + 1;

							//--- Organization id
							name = json.substring(begin, index).trim();
							name = name.replaceAll("\"", "");
							
							ZammadEntry entry = parseOrganization(json, in);
							list.add(entry);

						}
						
					} else if (name.equals("Group")) {
						in.increment(); //--- Skip first {
						while (in.index < json.length()) {
							c = json.charAt(in.index);
							if (c == '}') {
								in.increment();
								break;

							} else if (c == ',') {
								//--- Read next field
								in.increment();
								continue;
							}

							begin = in.index;
							index = json.indexOf(':', in.index);
							if (index == -1) break;
							in.index = index + 1;

							//--- Group id
							name = json.substring(begin, index).trim();
							name = name.replaceAll("\"", "");
							
							ZammadEntry entry = parseGroup(json, in);
							list.add(entry);

						}
					}
				}

			}
			// System.out.println("<<< END PARSING ENTRY");

		}
		return list;
	}

	/**
	 * Parse entry until } is found, the first char is {
	 *
	 * {"id":3,"group_id":1,"priority_id":2,"state_id":4,"organization_id":null,"number":"62003","title":"Example","owner_id":1,"customer_id":4,"note":null,"first_response_at":"2020-11-20T11:08:31.565Z","first_response_escalation_at":null,"first_response_in_min":null,"first_response_diff_in_min":null,"close_at":"2020-11-24T17:12:06.645Z","close_escalation_at":null,"close_in_min":null,"close_diff_in_min":null,"update_escalation_at":null,"update_in_min":null,"update_diff_in_min":null,"last_contact_at":"2020-11-20T11:08:31.565Z","last_contact_agent_at":"2020-11-20T11:08:31.565Z","last_contact_customer_at":null,"last_owner_update_at":null,"create_article_type_id":1,"create_article_sender_id":1,"article_count":1,"escalation_at":null,"pending_time":null,"type":null,"time_unit":null,"preferences":{"channel_id":3},"updated_by_id":4,"created_by_id":4,"created_at":"2020-11-20T11:08:31.538Z","updated_at":"2020-12-04T08:50:10.562Z"}
	 *
	 * @param json
	 */
	private ZammadEntry parseEntry(String json, Index in) {
		// System.out.println(">>> PARSING ENTRY");
		ZammadEntry entry = new ZammadEntry();
		in.index++;	//--- skip first {
		while (in.index < json.length()) {
			//--- Check special case were the entry is empty
			char c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;
			}

			//--- Find first separator
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			Object value = "";

			in.index = index + 1;
			c = json.charAt(in.index);
			// System.out.println("[" + name + "]VALUE FIRST CHAR:" + c);
			if (c == '{') {
				//--- Another entry
				value = parseEntry(json, in);

			} else if (c == '[') {
				//--- Another array
				value = parseArrays(json, in);

			} else if (c == '"') {
				value = parseString(json, in);

			} else {
				value = parseNonString(json, in);

			}

			entry.setField(name.trim(), value);

			c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;

			} else if (c == ',') {
				//-- Read next field
				in.index++;

			}
		}
		// System.out.println("<<< END PARSING ENTRY");
		return entry;
	}

	private ZammadEntry parseUser(String json, Index in) {
		// System.out.println(">>> PARSING ENTRY");
		ZammadEntry entry = new ZammadEntry();
		in.index++;	//--- skip first {
		while (in.index < json.length()) {
			//--- Check special case were the entry is empty
			char c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;
			}

			//--- Find first separator
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			Object value = "";

			in.index = index + 1;
			c = json.charAt(in.index);
			// System.out.println("[" + name + "]VALUE FIRST CHAR:" + c);
			if (c == '{') {
				//--- Another entry
				value = parseUser(json, in);

			} else if (c == '[') {
				//--- Another array
				value = parseArrays(json, in);

			} else if (c == '"') {
				value = parseString(json, in);

			} else {
				value = parseNonString(json, in);

			}

			entry.setField(name.trim(), value);

			c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;

			} else if (c == ',') {
				//-- Read next field
				in.index++;

			}
		}
		// System.out.println("<<< END PARSING ENTRY");
		return entry;
	}
	
	private ZammadEntry parseRole(String json, Index in) {
		// System.out.println(">>> PARSING ENTRY");
		ZammadEntry entry = new ZammadEntry();
		in.index++;	//--- skip first {
		while (in.index < json.length()) {
			//--- Check special case were the entry is empty
			char c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;
			}

			//--- Find first separator
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			Object value = "";

			in.index = index + 1;
			c = json.charAt(in.index);
			// System.out.println("[" + name + "]VALUE FIRST CHAR:" + c);
			if (c == '{') {
				//--- Another entry
				value = parseRole(json, in);

			} else if (c == '[') {
				//--- Another array
				value = parseArrays(json, in);

			} else if (c == '"') {
				value = parseString(json, in);

			} else {
				value = parseNonString(json, in);

			}

			entry.setField(name.trim(), value);

			c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;

			} else if (c == ',') {
				//-- Read next field
				in.index++;

			}
		}
		// System.out.println("<<< END PARSING ENTRY");
		return entry;
	}
	
	private ZammadEntry parseOrganization(String json, Index in) {
		// System.out.println(">>> PARSING ENTRY");
		ZammadEntry entry = new ZammadEntry();
		in.index++;	//--- skip first {
		while (in.index < json.length()) {
			//--- Check special case were the entry is empty
			char c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;
			}

			//--- Find first separator
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			Object value = "";

			in.index = index + 1;
			c = json.charAt(in.index);
			// System.out.println("[" + name + "]VALUE FIRST CHAR:" + c);
			if (c == '{') {
				//--- Another entry
				value = parseOrganization(json, in);

			} else if (c == '[') {
				//--- Another array
				value = parseArrays(json, in);

			} else if (c == '"') {
				value = parseString(json, in);

			} else {
				value = parseNonString(json, in);

			}

			entry.setField(name.trim(), value);

			c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;

			} else if (c == ',') {
				//-- Read next field
				in.index++;

			}
		}
		// System.out.println("<<< END PARSING ENTRY");
		return entry;
	}
	
	private ZammadEntry parseGroup(String json, Index in) {
		// System.out.println(">>> PARSING ENTRY");
		ZammadEntry entry = new ZammadEntry();
		in.index++;	//--- skip first {
		while (in.index < json.length()) {
			//--- Check special case were the entry is empty
			char c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;
			}

			//--- Find first separator
			int begin = in.index;
			int index = json.indexOf(':', in.index);
			if (index == -1) break;

			//--- Determine field name and value
			String name = json.substring(begin, index).trim();
			name = name.replaceAll("\"", "");
			Object value = "";

			in.index = index + 1;
			c = json.charAt(in.index);
			// System.out.println("[" + name + "]VALUE FIRST CHAR:" + c);
			if (c == '{') {
				//--- Another entry
				value = parseGroup(json, in);

			} else if (c == '[') {
				//--- Another array
				value = parseArrays(json, in);

			} else if (c == '"') {
				value = parseString(json, in);

			} else {
				value = parseNonString(json, in);

			}

			entry.setField(name.trim(), value);

			c = json.charAt(in.index);
			if (c == '}') {
				in.index++;
				break;

			} else if (c == ',') {
				//-- Read next field
				in.index++;

			}
		}
		// System.out.println("<<< END PARSING ENTRY");
		return entry;
	}
	
	
	/**
	 * The first is the " The index will be the one after the last " after
	 * returning
	 *
	 * @param json
	 */
	private String parseString(String json, Index in) {
		in.index++;	//--- skip first "
		int begin = in.index;
		int end = in.index;
		while (in.index < json.length()) {
			char c = json.charAt(in.index);
			// System.out.println("READ:"+c+" (int) "+(int) c);
			if (c == '\\') {
				in.index++;
				c = json.charAt(in.index);
				if (c == '"') in.index++;

			} else if (c == '"') {
				//--- Found the end
				end = in.index;
				in.index++;	//-- Skip it
				break;

			} else {
				in.index++;
			}
		}
		return json.substring(begin, end);
	}

	/**
	 * Return a non string value, the index is after the value after returning
	 *
	 * @param json
	 * @param in
	 * @return
	 */
	private String parseNonString(String json, Index in) {
		int begin = in.index;
		int end = in.index;
		while (in.index < json.length()) {
			char c = json.charAt(in.index);
			if (c == ',') {
				end = in.index;
				break;

			} else if (c == '}') {
				end = in.index;
				break;

			} else if (c == ']') {
				end = in.index;
				break;

			} else {
				in.index++;
			}
		}
		return json.substring(begin, end);
	}

	//--- Utility class for storing the current json parsing index
	private class Index {

		protected int index = 0;

		private Index() {
			//---
		}

		public void increment() {
			index = index + 1;
		}

		public void increment(int inc) {
			index = index + inc;
		}

	}
}
