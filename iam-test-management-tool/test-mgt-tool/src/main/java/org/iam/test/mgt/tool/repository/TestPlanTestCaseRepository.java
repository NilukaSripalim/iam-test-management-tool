package org.iam.test.mgt.tool.repository;

import org.iam.test.mgt.tool.domain.EcTestcase;
import org.iam.test.mgt.tool.domain.EcTestplan;
import org.iam.test.mgt.tool.domain.EcTestplanTestcaseMapping;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface TestPlanTestCaseRepository extends CrudRepository<EcTestplanTestcaseMapping, Long> {

    @Query(value = "select b.* from ec_testplan a, ec_testplan_testcase_mapping b,ec_testcase c where a.id = b.testplan_id and b.testcase_id = c.id and a.enabled = true and c.folder_id = :folderId", nativeQuery = true)
    public List<EcTestplanTestcaseMapping> findAllByTestFolderId(@Param("folderId") Long folderId);

    public EcTestplanTestcaseMapping findByTestplanIdAndTestcaseId(EcTestplan tp, EcTestcase tc);

    public List<EcTestplanTestcaseMapping> findByTestplanId(EcTestplan tp);

    public List<EcTestplanTestcaseMapping> findByTestcaseId(EcTestcase tc);

    @Query(value = "SELECT a.* FROM excord.ec_testplan_testcase_mapping a, ec_testcase b where a.testcase_id = b.id and a.testplan_id = :testplanId and b.folder_id = :folderId", nativeQuery = true)
    public List<EcTestplanTestcaseMapping> findByTestplanIdAndFolderId(@Param("testplanId") Long testplanId,@Param("folderId") Long folderId);

}
