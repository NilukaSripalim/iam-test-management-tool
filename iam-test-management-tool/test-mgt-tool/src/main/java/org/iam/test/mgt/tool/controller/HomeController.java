package org.iam.test.mgt.tool.controller;

import org.iam.test.mgt.tool.domain.EcRequirement;
import org.iam.test.mgt.tool.repository.RequirementRepository;
import org.iam.test.mgt.tool.repository.TestCaseRepository;
import org.iam.test.mgt.tool.repository.TestPlanRepository;
import org.iam.test.mgt.tool.repository.TestResultRepository;
import org.iam.test.mgt.tool.utils.BizUtil;
import org.iam.test.mgt.tool.utils.FlashMsgUtil;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpSession;

import org.iam.test.mgt.tool.utils.BizUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    TestCaseRepository tcDao;

    @Autowired
    RequirementRepository rDao;

    @Autowired
    TestPlanRepository tpDao;

    @Autowired
    TestResultRepository trDao;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Model model, HttpSession session) {
        FlashMsgUtil.INSTANCE.checkFlashMsg(session, model);

        EcRequirement currReq = rDao.findByParentIdIsNull();
        List<Long> listOfAllChildReqLst = BizUtil.INSTANCE.getListOfAllChildReq(currReq);
        if (listOfAllChildReqLst == null) {
            listOfAllChildReqLst = new ArrayList<>();
        }
        if (currReq.getCoverage()) {
            listOfAllChildReqLst.add(currReq.getId());
        }
        List<EcRequirement> reqMissingCoverageLst = rDao.findAllMissingCoverage();
        Integer missCnt = 0;
        for (EcRequirement coverageReq : reqMissingCoverageLst) {
            if (listOfAllChildReqLst.contains(coverageReq.getId())) {
                missCnt++;
            }
        }
        Integer totalReqCnt = listOfAllChildReqLst.size();
        Long coveragePercentage = Math.round(((totalReqCnt - missCnt) * 100.0) / totalReqCnt);

        Integer automationCnt = tcDao.automationCnt();
        Integer testcaseCnt = tcDao.getCountOfActiveTestcases();
        Long automationPercentage = Math.round((automationCnt * 100.0) / testcaseCnt);

        model.addAttribute("tcCnt", testcaseCnt);
        model.addAttribute("tpCnt", tpDao.getCountOfActiveTestplan());
        model.addAttribute("rCnt", rDao.getCountOfActiveRequirements());
        model.addAttribute("trCnt", trDao.getCountOfExecutionByYear());
        model.addAttribute("coveragePercentage", coveragePercentage);
        model.addAttribute("automationPercentage", automationPercentage);
        return "home";
    }

}
