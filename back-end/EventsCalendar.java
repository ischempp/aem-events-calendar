package org.fhcrc.centernet.components; 

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.Page;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.HashMap;
import java.util.Iterator;

import com.day.cq.search.QueryBuilder;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.result.Hit;
import com.day.cq.tagging.TagManager;
import com.day.cq.tagging.Tag;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Calendar;
import org.fhcrc.centernet.helper.EventHelper;
import org.fhcrc.common.util.TagUtilities;
import org.fhcrc.centernet.util.ListSorter;
import org.fhcrc.centernet.Constants;

/**
 * @author ischempp
 */

public class EventsCalendar extends WCMUse {
	final static Logger log = LoggerFactory.getLogger(EventsCalendar.class);

	final String PN_QUERY_START_TIME = "@jcr:content/eventdetails/start";
	final String PN_START_TIME = "start";
	final String PN_END_TIME = "end";
	final String PN_TITLE = "jcr:title";
	final String PN_SPEAKER = "speaker";
	final String PN_LOCATION = "location";
	final String PN_LOCATION_MAP = "locationMap";
	final String PN_HOST = "host";
	final String PN_CONTACT = "text";
	final String PN_TAG_NAME = "name";
	final String PN_TAG_TITLE = "title";
	final String PN_IS_EXTERNAL = "isExternal";
	
	private ResourceResolver resResolver;
	private TagManager tagManager;
	private QueryBuilder queryBuilder;
	private HashMap<String,String> queryMap;
	private Session session;
	private List<Map<String, String>> itemList;
	private List<Hit> hitList;
	private List<Map<String, String>> departmentMap;
	private List<Map<String, String>> categoryMap;
	
	/**
	 * Activate AnnouncementLister Class.
	 */
	@Override
	public void activate() throws java.lang.Exception{
		
		resResolver = getResourceResolver();
		session = resResolver.adaptTo(Session.class);
		queryBuilder = resResolver.adaptTo(QueryBuilder.class);
		tagManager = resResolver.adaptTo(TagManager.class);
		departmentMap = new ArrayList<Map<String, String>>();
		categoryMap = new ArrayList<Map<String, String>>();
		
		queryMap = createQueryMap();
		hitList = queryBuilder.createQuery(PredicateGroup.create(queryMap), session).getResult().getHits();
		
		itemList = createItemList();
		categoryMap = ListSorter.sortListByKey(categoryMap, PN_TAG_TITLE);
		departmentMap = ListSorter.sortListByKey(departmentMap, PN_TAG_TITLE);
		
	}
	
	public Boolean isEmpty() {
		
		return itemList.isEmpty();
		
	}
	
	public List<Map<String,String>> getItemList() {
		
		return itemList;
		
	}
	
	public List<Map<String, String>> getCategories() {
		
		return categoryMap;
		
	}
	
	public List<Map<String, String>> getDepartments() {
		
		return departmentMap;
		
	}
	
	/**
	 * Creates a Map to be used by the AEM QueryBuilder to find all Events
	 * in the CenterNet website.
	 * @return a Map to be used by the QueryBuilder to execute a Query
	 */
	private HashMap<String,String> createQueryMap() {
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		// Only include Pages with the CenterNet Event template
		map.put("type","cq:Page");
		map.put("path", Constants.EVENTS);
		map.put("property","jcr:content/cq:template");
	    map.put("property.value", Constants.EVENT_TEMPLATE);
	    
	    // Only include Events whose start time is in the past thirty days or 
	    // in the future
	    map.put("relativedaterange.property", PN_QUERY_START_TIME);
	    map.put("relativedaterange.lowerBound", "-30d");
	    
	    // Include all hits
	    map.put("p.limit", "-1");
	    map.put("p.guessTotal", "true");
	    
	    // Order by Start Time
	    map.put("orderby", PN_QUERY_START_TIME);
	    map.put("orderby.sort", "asc");
	    
		return map;
		
	}
	
	/**
	 * Creates a list to be used by Sightly's data-use-list functionality
	 * @return a list of all Events in the system
	 */
	private List<Map<String,String>> createItemList() {
		
		ArrayList<Map<String,String>> itemList = new ArrayList<Map<String,String>>();
		Iterator<Hit> hits = hitList.iterator();
		
		while (hits.hasNext()) {
			
			try {
				
				Page p = hits.next().getResource().adaptTo(Page.class);
				Map<String,String> map = new HashMap<String,String>();
				Resource eventdetail = p.getContentResource().getChild("eventdetails");
				Boolean hasDetails = eventdetail != null;
				String title = new String(),
						summary = new String(),
						speaker = new String(),
						location = new String(),
						locationMap = new String(),
						host = new String(),
						contactName = new String(),
						contactEmail = new String(),
						contactPhone = new String(),
						legacyContact = new String(),
						time = new String(),
						unixtime = new String(),
						categories = new String(),
						depts = new String(),
						deptsData = new String(),
						startDate = new String(),
						external = new String();
				
				title = p.getNavigationTitle() != null ? p.getNavigationTitle() : p.getTitle();
				
				// Get the page tags and check to see if there are any from the
				// departments or category namespaces
				ArrayList<Tag> catList = TagUtilities.getTagsByNamespace(p, tagManager.resolve(Constants.EVENT_CATEGORY_TAG));
				addCategories(catList);
				categories = createTagString(catList, PN_TAG_NAME);
				
				ArrayList<Tag> deptList = TagUtilities.getTagsByNamespace(p, tagManager.resolve(Constants.DEPARTMENT_TAG));
				setDepartmentTags(deptList);
				depts = createTagString(deptList, PN_TAG_TITLE);
				deptsData = createTagString(deptList, PN_TAG_NAME);
				
				if (hasDetails) {
					
					ValueMap properties = eventdetail.getValueMap();
					
					summary = p.getDescription();
					location = properties.get("location","");
					speaker = properties.get("speaker","");
					locationMap = properties.get("locationMap","");
					host = properties.get("host","");
					contactName = properties.get("contactName","");
					contactEmail = properties.get("contactEmail","");
					contactPhone = properties.get("contactPhone","");
					// legacyContact was the old way of holding contact information
					legacyContact = properties.get("contact","");
					time = createEventTimeString(eventdetail);
					unixtime = EventHelper.getUnixTime(eventdetail);
					startDate = EventHelper.getStartDate(eventdetail);
					external = properties.get("isExternal","false");
					
				}
				
				map.put("title", title);
				map.put("path", p.getPath());
				map.put("summary", summary);
				map.put("location", location);
				map.put("speaker", speaker);
				map.put("locationMap", locationMap);
				map.put("host", host);
				// If you do have legacy contact info and don't have new contact info, then use the legacy info
				if (!StringUtils.isBlank(legacyContact) && 
						(StringUtils.isBlank(contactName) && 
						StringUtils.isBlank(contactEmail) && 
						StringUtils.isBlank(contactPhone))) {
					map.put("contact", legacyContact);
				} else {
					map.put("contact", EventHelper.getFormattedContactInfo(contactName, contactEmail, contactPhone));
				}
				map.put("time", time);
				map.put("unixtime", unixtime);
				map.put("categories", categories);
				map.put("departments", depts);
				map.put("departmentsData", deptsData);
				map.put("startDate", startDate);
				map.put("isExternal", external);
				
				itemList.add(map);

			} catch (RepositoryException e) {

				log.error("EVENT CALENDAR: problem adapting hit to page: " + e.getMessage());
				
			}
			
		}
		
		return itemList;
		
	}
	
	/**
	 * Takes in a List of Tags and checks to see if the paths of those Tags are
	 * already in the variable categoryList. If not, then those paths are 
	 * added.
	 * @param list - A list of tags to potentially add to the category list
	 */
	private void addCategories(List<Tag> l) {
    	
    	Iterator<Tag> catIterator = l.iterator();
    	
    	while (catIterator.hasNext()) {
    		
    		Tag t = catIterator.next();
    		Map<String, String> m = new HashMap<String, String>();
    		m.put(PN_TAG_NAME, t.getName());
    		m.put(PN_TAG_TITLE, t.getTitle());
    		
    		if (categoryMap.isEmpty() || !categoryMap.contains(m)) {
    			categoryMap.add(m);
    		} 
    		
    	}
		
	}
	
	/**
	 * Takes in a List of Tags and checks to see if the paths of those Tags are
	 * already in the variable departmentList. If not, then those paths are 
	 * added.
	 * @param list - A list of tags to potentially add to the department list
	 */
	private void setDepartmentTags(List<Tag> l) {
    	
    	Iterator<Tag> deptIterator = l.iterator();
    	
    	while (deptIterator.hasNext()) {
    		
    		Tag t = deptIterator.next();
    		Map<String, String> m = new HashMap<String, String>();
    		m.put(PN_TAG_NAME, t.getName());
    		m.put(PN_TAG_TITLE, t.getTitle());
    		
    		if (departmentMap.isEmpty() || !departmentMap.contains(m)) {
    			departmentMap.add(m);
    		} 
    		
    	}
    	
    }
	
	/**
	 * Returns HTML to be printed onto the events calendar page
	 * @param eventdetails - The Event Details component of the Event in
	 * question
	 * @return String timeString - HTML-formatted string representing the
	 * time duration of the Event in question 
	 */
	private String createEventTimeString(Resource eventdetails) {
		
		String timeString = "";
		StringBuffer sb = new StringBuffer();
		
		Calendar startDate = eventdetails.getValueMap().get("start", Calendar.class);
		Calendar endDate = eventdetails.getValueMap().get("end", Calendar.class);
		
		// Check for both start date and end date
		if (startDate == null || endDate == null) {
			
			log.error("EVENT CALENDAR: Event with no start or end date encountered.");
			timeString = "Cannot determine";
			return timeString;
			
		}
		
		// Error case: start date is after end date, chronologically
		// Solution: return start date/time only
		if (startDate.compareTo(endDate) > 0) {
			
			log.warn("EVENT CALENDAR: Start date occurs after end date. Returning Start date only.");

			sb.append("<span class=\"event-time\">");
			sb.append("<span class=\"day\">" + EventHelper.getDayOfWeek(startDate) + "</span>");
			sb.append("<br>");
			sb.append(EventHelper.getMonth(startDate));
			sb.append(" ");
			sb.append(EventHelper.getDate(startDate));
			sb.append("</span>");
			sb.append("<br>");
			sb.append(EventHelper.getTime(startDate));
			sb.append("&nbsp;");
			sb.append(EventHelper.getAmPm(startDate));
			return sb.toString();
			
		}
		
		// Expected state: start date and end date are same day, end time is
		// later than start time
		if (startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)
				&& startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR)) {
			
			String startTime = EventHelper.getTime(startDate);
			String endTime = EventHelper.getTime(endDate);
			
			sb.append("<span class=\"event-time\">");
			sb.append("<span class=\"day\">" + EventHelper.getDayOfWeek(startDate) + "</span>");
			sb.append("<br>");
			sb.append(EventHelper.getMonth(startDate));
			sb.append(" ");
			sb.append(EventHelper.getDate(startDate));
			sb.append("</span>");
			sb.append("<br>");
			sb.append(startTime);
			
			if (!EventHelper.getAmPm(startDate).equals(EventHelper.getAmPm(endDate)) && 
				!startTime.equals("noon")) {
				
				sb.append("&nbsp;");
				sb.append(EventHelper.getAmPm(startDate));
				
			}
			
			sb.append(" - ");
			sb.append(endTime);
			
			if (!endTime.equals("noon")) {
				
				sb.append("&nbsp;");
				sb.append(EventHelper.getAmPm(endDate));
				
			}
			
			return sb.toString();
			
		} else {
			
			// Error case: end date is different than start date
			// Solution: return 'Multi-day' as date string
			log.warn("EVENT CALENDAR: Multi-day event found. Can not handle.");
			timeString = "Multi-day";
			return timeString;
			
		}
		
	}
	
	/**
	 * Gives you a String containing a comma-separated list of the names of
	 * the Tags contained in the provided ArrayList.
	 * @param list A list of tags whose names should be added to the string
	 * @return A comma-separated list of tag names
	 */
	private String createTagString(ArrayList<Tag> tagList, String mode) {	
		
		if (!mode.equals(PN_TAG_TITLE) && !mode.equals(PN_TAG_NAME)) {
			log.error("Error creating tag string. Illegal mode passed.");
			return "";
		}
		
		StringBuffer sb = new StringBuffer();
		Iterator<Tag> ti = tagList.iterator();
		
		while (ti.hasNext()) {			
			Tag t = ti.next();
			if (mode.equals(PN_TAG_TITLE)) {
				sb.append(t.getTitle());
			} else if (mode.equals(PN_TAG_NAME)) {
				sb.append(t.getName());
			}
			
			if (ti.hasNext()) {
				sb.append(", ");
			}
			
		}
		
		return sb.toString();
		
	}
	
	/**
	 * Debugging function
	 */
//	private void debug() {
//		
//		Integer s = hitList.size();
//		log.info("EVENT CALENDAR: hit list size = " + s.toString());
//		
//	}
	
}
