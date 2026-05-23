package com.union.accounting.model;

import java.util.List;

public class ActivityDetail {
    public GroupActivity activity;
    public List<ActivityMember> members;
    public Collect collect;
    public List<Pay> pays;
    public Boolean isCreator;
}
