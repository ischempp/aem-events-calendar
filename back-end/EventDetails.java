package org.fhcrc.centernet.components; 

import javax.jcr.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.foundation.Download;
import com.day.text.Text;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.Calendar;
import org.fhcrc.centernet.helper.EventHelper;
import org.fhcrc.centernet.util.PhoneFormatter;

/**
 * @author ischempp
 */

public class EventDetails extends WCMUse {
	final static Logger log = LoggerFactory.getLogger(EventsCalendar.class);
	private final String PN_CONTACT_NAME = "contactName";
	private final String PN_CONTACT_EMAIL = "contactEmail";
	private final String PN_CONTACT_PHONE = "contactPhone";
	
	private Resource resource;
	private ValueMap properties;
	private String timeString = "";
	private String attachmentLink = "";
	private String phone = "";
	private String contact = "";
	private Calendar startTime;
	private Calendar endTime;
  private String fileRef = "";
	
	/**
	 * Activate AnnouncementLister Class.
	 */
	@Override
	public void activate() throws java.lang.Exception{
		
		resource = getResource().getChild("attachment");
		properties = getProperties();
		startTime = properties.get("start", Calendar.class);
		endTime = properties.get("end", Calendar.class);
		contact = EventHelper.getFormattedContactInfo(
				properties.get(PN_CONTACT_NAME, ""), 
				properties.get(PN_CONTACT_EMAIL, ""), 
				properties.get(PN_CONTACT_PHONE, ""));
		setTimeString();
		setAttachmentLink();
		setFileRef();
	}
	
	public String getTimeString()  {
		
		return timeString;
		
	}
	
	public String getAttachmentLink() {
		
		return attachmentLink;
		
	}
	
	public String getPhone() {
		
		return phone;
		
	}
	
	public String getContactInfo() {
		
		return contact;
		
	}
	
	private void setAttachmentLink() {
		if (null != resource) {
      Download dld = new Download(resource);

      if (dld.hasContent()) {

        attachmentLink = Text.escape(dld.getHref(), '%', true);

      }
    }
	}
  
  private void setFileRef() {
    if (null != resource) {
      try {
        Node attachmentNode = resource.adaptTo(Node.class);
        if (attachmentNode.hasProperty("fileReference")) {
          fileRef = attachmentNode.getProperty("fileReference").getValue().toString();
        }
      } catch (Exception e) {
        log.error("Fatal JCR node error while setting the DAM file reference used for posting the attachment to the listserv: " + e.getMessage());
      } 
    }
  }
  
  public String getFileRef() {
    return fileRef;
  }
	
	private void setTimeString() {
		
		// Check for both start and end dates
		if (startTime == null || endTime == null) {
			
			timeString = "Please set a Start and End time for this event.";
			return;
			
		}
		
		// Error case: start date is AFTER end date
		if (startTime.compareTo(endTime) > 0) {
			
			log.warn("EVENT DETAIL - Start time occurs after end time");
			timeString = "WARNING: Start time occurs AFTER end time. Please reconfigure component";
			return;
			
		}
		
		// Expected Case: start date and end date are the same day
		if (startTime.get(Calendar.DAY_OF_YEAR) == endTime.get(Calendar.DAY_OF_YEAR)
				&& startTime.get(Calendar.YEAR) == endTime.get(Calendar.YEAR)) {
			
			StringBuffer sb = new StringBuffer();
			
			sb.append(EventHelper.getDayOfWeek(startTime));
			sb.append(", ");
			sb.append(EventHelper.getMonth(startTime));
			sb.append(" ");
			sb.append(EventHelper.getDate(startTime));
			sb.append(", ");
			sb.append(EventHelper.getYear(startTime));
			sb.append(", ");
			sb.append(EventHelper.getTime(startTime));
			
			if (!EventHelper.getAmPm(startTime).equals(EventHelper.getAmPm(endTime)) && 
				!EventHelper.getTime(startTime).equals("noon")) {
				
				sb.append(" ");
				sb.append(EventHelper.getAmPm(startTime));
				
			}
			
			sb.append(" - ");
			sb.append(EventHelper.getTime(endTime));
			
			if (!EventHelper.getTime(endTime).equals("noon")) {
				
				sb.append(" ");
				sb.append(EventHelper.getAmPm(endTime));
				
			}
			
			timeString = sb.toString();
			
		} else {
			
			// Multi-Day event. Currently not handled.
			log.warn("EVENT DETAIL: Multi-day event");
			timeString = "WARNING: This event spans multiple days, which is currently unsupported. Please reconfigure component.";
			return;
			
		}
		
	}
	
}
