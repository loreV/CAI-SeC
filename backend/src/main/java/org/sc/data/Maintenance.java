package org.sc.data;

import java.util.Date;

public class Maintenance {

    public static final String COLLECTION_NAME = "core.Maintenance";

    public static final String OBJECT_ID = "_id";
    public static final String TRAIL_CODE = "code";
    public static final String DATE = "date";
    public static final String DESCRIPTION = "description";
    public static final String CONTACT = "contact";
    public static final String MEETING_PLACE = "meetingPlace";

    private String _id;
    private Date date;
    private String code;
    private String meetingPlace;
    private String description;
    private String contact;

    public Maintenance(String id,
                       Date date,
                       String code,
                       String meetingPlace,
                       String description,
                       String contact) {
        _id = id;
        this.date = date;
        this.code = code;
        this.meetingPlace = meetingPlace;
        this.description = description;
        this.contact = contact;
    }

    public Date getDate() {
        return date;
    }

    public String getCode() {
        return code;
    }

    public String getMeetingPlace() {
        return meetingPlace;
    }

    public String getDescription() {
        return description;
    }

    public String getContact() {
        return contact;
    }

    public String getId() {
        return _id;
    }

}
