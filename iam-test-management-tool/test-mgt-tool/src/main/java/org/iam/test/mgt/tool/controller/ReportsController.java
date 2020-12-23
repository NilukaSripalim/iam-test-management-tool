package org.iam.test.mgt.tool.controller;

import org.iam.test.mgt.tool.domain.EcRequirement;
import org.iam.test.mgt.tool.domain.EcTestcaseRequirementMapping;
import org.iam.test.mgt.tool.repository.RequirementRepository;
import org.iam.test.mgt.tool.repository.TestPlanRepository;
import org.iam.test.mgt.tool.repository.TestResultRepository;
import org.iam.test.mgt.tool.repository.TestcaseRequirementRepository;
import org.iam.test.mgt.tool.utils.BizUtil;
import org.iam.test.mgt.tool.vo.TestPlanMetricVo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReportsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportsController.class);

    @Autowired
    TestPlanRepository tpDao;

    @Autowired
    TestResultRepository trDao;

    @Autowired
    TestcaseRequirementRepository tcrDao;

    @Autowired
    RequirementRepository rDao;

    @RequestMapping(value = "/report_execution", method = RequestMethod.GET)
    public String reportExecution(Model model) {
        List<Object[]> topTesterMonthLst = trDao.findByTopTesterByEnvByMonth();
        List<Object[]> topTesterYearLst = trDao.findByTopTesterByEnvByYear();
        List<Object[]> mostExeTestsLst = trDao.mostexecutedManualTests();

        model.addAttribute("topTesterMonthLst", topTesterMonthLst);
        model.addAttribute("topTesterYearLst", topTesterYearLst);
        model.addAttribute("mostExeTestsLst", mostExeTestsLst);
        return "report_testrun";
    }

    @RequestMapping(value = "/testplan_metric", method = RequestMethod.GET)
    public String testplanMetric(Model model, @RequestParam(value = "testplanId", required = true) Long testplanId) {
        List<Object[]> metricLst = tpDao.findByPriorityByTester(testplanId);
        List<Object[]> tmLst = tpDao.findMetricsByTestplanId(testplanId);
        List<TestPlanMetricVo> testPlanMetricLst = BizUtil.INSTANCE.flattenTestPlanMetricsByFolder(tmLst);
        model.addAttribute("testPlanMetricLst", testPlanMetricLst);
        model.addAttribute("metricLst", metricLst);
        return "testplan_metric";
    }

    @RequestMapping(value = "/report_review", method = RequestMethod.GET)
    public String reportReview(Model model) {
        List<EcTestcaseRequirementMapping> reviewLst = tcrDao.findAllByReviewTrue();
        model.addAttribute("reviewLst", reviewLst);
        return "report_review";
    }

    @RequestMapping(value = "/report_coverage", method = RequestMethod.GET)
    public String reportCoverage(Model model) {
        List<EcRequirement> reqLst = rDao.findAllMissingCoverage();
        model.addAttribute("reqLst", reqLst);
        return "report_coverage";
    }

}
