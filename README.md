# JZammad

Java connector to parse Zammad ticket via the json api

Here is an example how to use the connector


    String token = "";

		ZammadConnectionManager zammad = new ZammadConnectionManager("https", "tickets.xxx.xxx", token, "/api/v1");
		zammad.dumpResponse(true);
		zammad.start();

		ZammadRequest req = new ZammadRequest("none", "tickets", "");
		req.setSortBy("number", "desc");
		req.setLimit(1, 100);
		// ZammadRequest req = new ZammadRequest("none", "ticket_articles/by_ticket/16", "");
		ZammadResponse response = zammad.sendb(req);
		// System.out.println("RESPONSE:" + response);
		ArrayList<Object> entries = response.getEntries();
		for (int i = 0;i < entries.size();i++) {
			Object obj = entries.get(i);
			if (obj instanceof ZammadEntry) {
				ZammadEntry entry = (ZammadEntry) obj;
				System.out.println("TICKET:" + entry.getField("number"));
				// System.out.println(entry);
				//System.out.println("===");
			}
		}
		zammad.interrupt();`
