package org.jenkinsci.plugins.urltrigger.service;

import com.sun.jersey.api.client.ClientResponse;
import org.jenkinsci.plugins.urltrigger.URLTriggerEntry;
import org.jenkinsci.plugins.urltrigger.URLTriggerException;
import org.jenkinsci.plugins.urltrigger.URLTriggerLog;
import org.jenkinsci.plugins.urltrigger.content.URLTriggerContentType;

import java.util.Date;

/**
 * @author Gregory Boissinot
 */
public class URLTriggerService {

    private static URLTriggerService INSTANCE = new URLTriggerService();

    private URLTriggerService() {
    }

    public static URLTriggerService getInstance() {
        return INSTANCE;
    }

    /**
     * @depreacated {@link ClientResponse#getEntity(Class)} can be called only once. Use {@link refreshContent(ClientResponse, String, URLTriggerEntry)}
     */
    @Deprecated
    public void refreshContent(ClientResponse clientResponse, URLTriggerEntry entry) throws URLTriggerException {
        initContent(clientResponse, clientResponse.getEntity(String.class), entry);
    }

    public void refreshContent(ClientResponse clientResponse, String stringContent, URLTriggerEntry entry) throws URLTriggerException {
        initContent(clientResponse, stringContent, entry);
    }

    /**
     * @depreacated {@link ClientResponse#getEntity(Class)} can be called only once. Use {@link initContent(ClientResponse, String, URLTriggerEntry)}
     */
    @Deprecated
    public void initContent(ClientResponse clientResponse, URLTriggerEntry entry) throws URLTriggerException {
        initContent(clientResponse, clientResponse.getEntity(String.class), entry);
    }

    public void initContent(ClientResponse clientResponse, String stringContent, URLTriggerEntry entry) throws URLTriggerException {

        if (clientResponse == null) {
            throw new NullPointerException("The given clientResponse object is not set.");
        }

        if (entry == null) {
            throw new NullPointerException("The given entry object is not set.");
        }

        Date lastModified = clientResponse.getLastModified();
        if (lastModified != null) {
            entry.setLastModificationDate(lastModified.getTime());
        } else {
            entry.setLastModificationDate(0);
        }

        if (entry.isInspectingContent()) {
            for (final URLTriggerContentType type : entry.getContentTypes()) {
                if (stringContent == null) {
                    throw new URLTriggerException("The URL content is empty.");
                }
                type.initForContent(stringContent);
            }
        }

    }

    /**
     * @depreacated {@link ClientResponse#getEntity(Class)} can be called only once. Use {@link isSchedulingForURLEntry(ClientResponse, String, URLTriggerEntry, URLTriggerLog)}
     */
    @Deprecated
    public boolean isSchedulingForURLEntry(ClientResponse clientResponse, URLTriggerEntry entry, URLTriggerLog log) throws URLTriggerException {
        return isSchedulingForURLEntry(clientResponse, clientResponse.getEntity(String.class), entry, log);
    }

    public boolean isSchedulingForURLEntry(ClientResponse clientResponse, String stringContent, URLTriggerEntry entry, URLTriggerLog log) throws URLTriggerException {
        //Get the url
        String url = entry.getUrl();

        //Check the status if needed
        if (entry.isCheckStatus()) {
            int status = clientResponse.getStatus();
            if (status == entry.getStatusCode()) {
                log.info(String.format("The returned status matches the expected status: \n %s", url));
                return true;
            }
        }

        //Check the last modified date if needed
        if (entry.isCheckLastModificationDate()) {
            Date lastModificationDate = clientResponse.getLastModified();
            if (lastModificationDate != null) {
                long newLastModifiedDate = lastModificationDate.getTime();
                long entryLastModificationDate = entry.getLastModificationDate();
                if (entryLastModificationDate == 0L) {
                    entry.setLastModificationDate(newLastModifiedDate);
                    return false;
                }
                if (entryLastModificationDate != newLastModifiedDate) {
                    entry.setLastModificationDate(newLastModifiedDate);
                    log.info("The last modification date has changed.");
                    return true;
                }
            }
        }

        //Check the url content
        if (entry.isInspectingContent()) {
            log.info("Inspecting the content");
            for (final URLTriggerContentType type : entry.getContentTypes()) {
                if (stringContent == null) {
                    throw new URLTriggerException("The URL content is empty.");
                }
                boolean isTriggered = type.isTriggeringBuildForContent(stringContent, log);
                if (isTriggered) {
                    return true;
                }
            }
        }

        return false;
    }

}
