package org.iam.test.mgt.tool.controller;

import org.iam.test.mgt.tool.domain.EcRequirement;
import org.iam.test.mgt.tool.domain.EcTestcase;
import org.iam.test.mgt.tool.domain.EcTestcaseRequirementMapping;
import org.iam.test.mgt.tool.repository.RequirementRepository;
import org.iam.test.mgt.tool.repository.TestCaseRepository;
import org.iam.test.mgt.tool.repository.TestcaseRequirementRepository;
import org.iam.test.mgt.tool.utils.BizUtil;
import org.iam.test.mgt.tool.utils.FlashMsgUtil;
import org.iam.test.mgt.tool.utils.HistoryUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RequirementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequirementController.class);

    @Autowired
    RequirementRepository rDao;

    @Autowired
    TestcaseRequirementRepository tcrDao;

    @Autowired
    TestCaseRepository tcDao;

    @Autowired
    HistoryUtil historyUtil;

    @RequestMapping(value = "/requirement", method = RequestMethod.GET)
    public String requirement(Model model, HttpSession session, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {

        FlashMsgUtil.INSTANCE.checkFlashMsg(session, model);
        EcRequirement currReq = rDao.findOne(reqId);
        EcRequirement parentReq = currReq.getParentId();
        if (parentReq == null) {
            parentReq = currReq;
        }
        EcRequirement tempReq = currReq;

        List<EcRequirement> parentReqLst = new ArrayList<>();
        parentReqLst.add(tempReq);
        while (tempReq.getParentId() != null) {
            parentReqLst.add(tempReq.getParentId());
            tempReq = tempReq.getParentId();
        }
        Collections.reverse(parentReqLst);

        Iterable<EcRequirement> nodeLst = rDao.findAllByParentIdOrderByNameAsc(currReq);
        List<Long> childReqLst = new ArrayList<>();
        for (EcRequirement d : nodeLst) {
            if (rDao.checkIfHasChildren(d.getId())) {
                childReqLst.add(d.getId());
            }
        }

        List<EcTestcaseRequirementMapping> tcrLst = tcrDao.findAllByRequirementId(currReq);
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

        model.addAttribute("currReq", currReq);
        model.addAttribute("parentReq", parentReq);
        model.addAttribute("parentReqLst", parentReqLst);
        model.addAttribute("nodeLst", nodeLst);
        model.addAttribute("childReqLst", childReqLst);
        model.addAttribute("tcrLst", tcrLst);
        model.addAttribute("childReqCnt", totalReqCnt);
        model.addAttribute("coveragePercentage", coveragePercentage);
        return "requirement";
    }

    @RequestMapping(value = "/testcase_requirement_link", method = RequestMethod.GET)
    public String testcaseRequirementLink(Model model, HttpSession session, HttpServletRequest request, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {
        Long nodeId = (Long) session.getAttribute("clipboardNodeId");
        String clipboardLinkTc = (String) session.getAttribute("clipboardLinkTc");
        if (clipboardLinkTc != null) {
            String[] clipboardLinkTcLst = StringUtils.commaDelimitedListToStringArray(clipboardLinkTc);
            for (String tcId : clipboardLinkTcLst) {

                EcTestcase tcObj = tcDao.findOne(Long.parseLong(tcId));
                EcRequirement reqObj = rDao.findOne(reqId);
                if (tcrDao.findByTestcaseIdAndRequirementId(tcObj, reqObj) == null) {
                    EcTestcaseRequirementMapping tcrObj = new EcTestcaseRequirementMapping();
                    tcrObj.setRequirementId(reqObj);
                    tcrObj.setTestcaseId(tcObj);
                    tcrObj.setReview(Boolean.FALSE);
                    tcrDao.save(tcrObj);
                    historyUtil.addHistory("Linked testcase: [" + tcObj.getName() + "] with requirement: [" + reqObj.getName() + "]", reqObj.getSlug(), request, session);
                }

            }
            session.setAttribute("flashMsg", "Successfully Linked testcases to requirement!");
            session.setAttribute("clipboardLinkTc", null);
            session.setAttribute("clipboardNodeId", null);
        }
        return "redirect:/testcase?nodeId=" + nodeId;
    }

    @RequestMapping(value = "/testcase_requirement_unlink", method = RequestMethod.GET)
    public String testcaseRequirementUnLink(Model model, HttpSession session, HttpServletRequest request, @RequestParam(value = "reqId", required = true) Long reqId, @RequestParam(value = "tcId", required = true) Long tcId) {

        EcTestcase tcObj = tcDao.findOne(tcId);
        EcRequirement reqObj = rDao.findOne(reqId);
        if (tcObj != null && reqObj != null) {
            tcrDao.deleteByTestcaseIdAndRequirementId(tcObj, reqObj);
            session.setAttribute("flashMsg", "Successfully Unlinked testcases from requirement!");
            historyUtil.addHistory("Unlinked testcase: [" + tcObj.getName() + "] from requirement: [" + reqObj.getName() + "]", reqObj.getSlug(), request, session);
        }
        return "redirect:/requirement?reqId=" + reqId;
    }

    @RequestMapping(value = "/testcase_requirement_link_cancel", method = RequestMethod.GET)
    public String testcaseRequirementLinkCancel(Model model, HttpSession session, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {
        Long nodeId = (Long) session.getAttribute("clipboardNodeId");
        session.setAttribute("clipboardLinkTc", null);
        session.setAttribute("clipboardNodeId", null);
        session.setAttribute("flashMsg", "Linking cancelled!");
        return "redirect:/testcase?nodeId=" + nodeId;
    }

    @RequestMapping(value = "/req_create", method = RequestMethod.GET)
    public String requirementCreate(Model model, HttpSession session, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {
        EcRequirement parentReq = rDao.findOne(reqId);
        model.addAttribute("parentReq", parentReq);
        return "requirement_form";
    }

    @RequestMapping(value = "/req_edit", method = RequestMethod.GET)
    public String requirementEdit(Model model, HttpSession session, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {
        EcRequirement req = rDao.findOne(reqId);
        model.addAttribute("req", req);
        model.addAttribute("parentReq", req.getParentId());
        return "requirement_form";
    }

    @RequestMapping(value = "/req_delete", method = RequestMethod.POST)
    public String requirementDelete(Model model, HttpSession session, HttpServletRequest request, @RequestParam(value = "reqId", required = false, defaultValue = "1") Long reqId) {

        EcRequirement reqObj = rDao.findOne(reqId);
        if (reqObj.getParentId() == null) {
            session.setAttribute("flashMsg", "Cant delete root requirement!");
            return "redirect:/requirement?reqId=" + reqId;
        }
        //if (!req.getEcRequirementList().isEmpty()) {
        //    session.setAttribute("flashMsg", "Cant delete requirements with child requirements. Delete the child requirements prior to delete!");
        //    return "redirect:/requirement?reqId=" + reqId;
        //}

        Long parentId = reqObj.getParentId().getId();
        String reqName = reqObj.getName();
        String reqSlug = reqObj.getSlug();
        rDao.delete(reqObj);
        historyUtil.addHistory("Deleted requirement: [" + reqName + "]", reqSlug, request, session);
        session.setAttribute("flashMsg", "Successfully deleted requirement!");
        return "redirect:/requirement?reqId=" + parentId;

    }

    @RequestMapping(value = "/requirement_save", method = RequestMethod.POST)
    public String requirementSave(Model model, HttpSession session, HttpServletRequest request,
                                  @RequestParam(value = "reqId", required = false) Long reqId,
                                  @RequestParam(value = "rParentId", required = true) Long rParentId,
                                  @RequestParam(value = "rname", required = true) String rname,
                                  @RequestParam(value = "rstatus", required = true) String rstatus,
                                  @RequestParam(value = "rcoverage", defaultValue = "false", required = true) Boolean rcoverage,
                                  @RequestParam(value = "rpriority", required = true) String rpriority,
                                  @RequestParam(value = "rrelease", required = false) String rrelease,
                                  @RequestParam(value = "rproduct", required = false) String rproduct,
                                  @RequestParam(value = "rstorypoint", required = true) Integer rstorypoint,
                                  @RequestParam(value = "rstory", required = true) String rstory
    ) {

        EcRequirement parentReq = null;
        if (!rParentId.equals(-1L)) {
            parentReq = rDao.findOne(rParentId);
        }
        EcRequirement reqObj = null;
        if (reqId != null) {
            reqObj = rDao.findOne(reqId);
        } else {
            reqObj = new EcRequirement();
            reqObj.setSlug(BizUtil.INSTANCE.getSlug());
        }
        reqObj.setParentId(parentReq);
        reqObj.setName(rname);
        reqObj.setStatus(rstatus);
        reqObj.setCoverage(rcoverage);
        reqObj.setPriority(rpriority);
        reqObj.setReleaseName(rrelease);
        reqObj.setProduct(rproduct);
        reqObj.setStoryPoint(rstorypoint);

        if (reqObj.getStory() != null) {
            if (!reqObj.getStory().equals(rstory)) {
                //Story changed then mark all testcases for review.
                tcrDao.updateAllLinkedTestcaseForReview(reqId);
            }
        }
        reqObj.setStory(rstory);
        rDao.save(reqObj);
        if (!rParentId.equals(-1L)) {
            historyUtil.addHistory("Saved requirement: [" + reqObj.getName() + "] under [" + parentReq.getName() + "]", reqObj.getSlug(), request, session);
        } else {
            historyUtil.addHistory("Saved requirement: [" + reqObj.getName() + "] under [ - ]", reqObj.getSlug(), request, session);
        }
        session.setAttribute("flashMsg", "Successfully saved requirement: " + rname);

        return "redirect:/requirement?reqId=" + reqObj.getId();
    }

    @RequestMapping(value = "/req_export", method = RequestMethod.GET)
    public void requirementExport(HttpServletResponse response, @RequestParam(value = "reqId", required = true, defaultValue = "1") Long reqId) {

        EcRequirement currenReq = rDao.findOne(reqId);
        List<EcRequirement> requirementLst = rDao.findAllByParentIdOrderByNameAsc(currenReq);
        if (requirementLst == null) {
            requirementLst = new ArrayList<>();
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Requirements");

        Map<Integer, Object[]> data = new TreeMap<>();
        Integer idx = 0;
        data.put(idx, new Object[]{"ID", "NAME", "PRIORITY", "STATUS", "RELEASE_NAME", "PRODUCT", "COVERAGE", "STORY_POINT", "STORY"});
        idx++;
        for (EcRequirement req : requirementLst) {
            data.put(idx, new Object[]{req.getId().toString(), req.getName(), req.getPriority(), req.getStatus(), req.getReleaseName(), req.getProduct(), req.getCoverage(), req.getStoryPoint(), req.getStory()});
            idx++;
        }

        for (Map.Entry<Integer, Object[]> entry : data.entrySet()) {
            Integer key = entry.getKey();
            Object[] objArr = entry.getValue();
            Row row = sheet.createRow(key);
            int cellnum = 0;
            for (Object obj : objArr) {
                Cell cell = row.createCell(cellnum++);
                //All values will be Strings.
                if (obj instanceof String) {
                    cell.setCellValue((String) obj);
                } else {
                    cell.setCellValue(obj.toString());
                }
            }

        }
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            response.setContentType("application/octet-stream");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
            String dateStr = sdf.format(cal.getTime());
            response.setHeader("Content-Disposition", "attachment; fileName=Requirements_" + currenReq.getId() + "_" + dateStr + ".xlsx");

            workbook.write(outputStream);
        } catch (Exception ex) {
            LOGGER.error("Req Export Error!", ex);
        }
    }

    @RequestMapping(value = "/req_upload", method = RequestMethod.POST)
    public String requirementUpload(Model model, HttpServletRequest request, HttpSession session, @RequestParam(value = "reqId", required = true) Long reqId, @RequestParam(value = "file") MultipartFile file) {
        Boolean statusError = false;
        Boolean priorityError = false;
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String fileName = file.getOriginalFilename();
                LOGGER.info("Uploading requirements file: {}", fileName);
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                XSSFWorkbook workbook = new XSSFWorkbook(bis);
                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                EcRequirement reqObj = rDao.findOne(reqId);
                Boolean skipHeader = true;
                final DataFormatter df = new DataFormatter();
                Long newReqId = -1L;

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (skipHeader) {
                        skipHeader = false;
                        continue;
                    }
                    EcRequirement childReq = null;
                    if (row.getCell(0) != null) {
                        newReqId = Long.parseLong(df.formatCellValue(row.getCell(0)));
                        childReq = rDao.findByIdAndParentId(newReqId, reqObj);
                    }
                    if (childReq == null) {
                        childReq = new EcRequirement();
                        childReq.setParentId(reqObj);
                        childReq.setSlug(BizUtil.INSTANCE.getSlug());
                    }

                    String rName = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(1)), 45);
                    String rPriority = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(2)), 45);
                    if (!BizUtil.INSTANCE.checkReqPriority(rPriority)) {
                        priorityError = true;
                        continue;
                    }
                    String rStatus = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(3)), 45);
                    if (!BizUtil.INSTANCE.checkReqStatus(rStatus)) {
                        statusError = true;
                        continue;
                    }

                    String rRelease = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(4)), 45);
                    String rProduct = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(5)), 45);
                    String rCoverage = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(6)), 5);
                    Integer rStoryPoint = Integer.parseInt(BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(7)), -1));
                    String rStory = BizUtil.INSTANCE.validateInput(df.formatCellValue(row.getCell(8)), -1);

                    if (rStoryPoint != 0 && rStoryPoint != 1 & rStoryPoint != 2 && rStoryPoint != 3 && rStoryPoint != 4 && rStoryPoint != 5) {
                        rStoryPoint = 0;
                    }

                    childReq.setName(rName);
                    childReq.setPriority(rPriority);
                    childReq.setStatus(rStatus);
                    childReq.setReleaseName(rRelease);
                    childReq.setProduct(rProduct);
                    childReq.setCoverage(Boolean.valueOf(rCoverage));
                    childReq.setStoryPoint(rStoryPoint);
                    if (childReq.getStory() != null) {
                        if (!childReq.getStory().equals(rStory)) {
                            //Story changed then mark all testcases for review.
                            tcrDao.updateAllLinkedTestcaseForReview(reqId);
                        }
                    }
                    childReq.setStory(rStory);

                    rDao.save(childReq);
                    historyUtil.addHistory("Requirement [" + childReq.getName() + "] added/updated by import, file: " + fileName, childReq.getSlug(), request, session);
                }

                if (statusError) {
                    session.setAttribute("flashMsg", "Invalid Status codes found in :" + fileName);
                } else if (priorityError) {
                    session.setAttribute("flashMsg", "Invalid Priority codes found in :" + fileName);
                } else {
                    session.setAttribute("flashMsg", "Successfully Imported :" + fileName);
                }

            } catch (IOException | NumberFormatException ex) {
                session.setAttribute("flashMsg", "File upload failed! " + ex.getMessage());
                LOGGER.error("Req Upload Error!", ex);
            }
        } else {
            session.setAttribute("flashMsg", "File is empty!");
        }
        return "redirect:/requirement?reqId=" + reqId;
    }

}
