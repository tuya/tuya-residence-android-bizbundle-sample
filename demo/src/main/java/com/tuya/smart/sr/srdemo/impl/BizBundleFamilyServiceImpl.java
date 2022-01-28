package com.tuya.smart.sr.srdemo.impl;

import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;

/**
 * @author xier
 * @desc
 * @time 2022/1/4 10:13
 */
public class BizBundleFamilyServiceImpl extends AbsBizBundleFamilyService {
    private long mHomeId;
    private String mHomeName;

    @Override
    public long getCurrentHomeId() {
        return mHomeId;
    }

    @Override
    public void shiftCurrentFamily(long familyId, String curName) {
        super.shiftCurrentFamily(familyId, curName);
        mHomeId = familyId;
        mHomeName = curName;
    }
}
