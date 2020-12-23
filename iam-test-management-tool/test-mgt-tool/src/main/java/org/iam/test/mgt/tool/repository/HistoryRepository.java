package org.iam.test.mgt.tool.repository;


import org.iam.test.mgt.tool.domain.EcHistory;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface HistoryRepository extends CrudRepository<EcHistory, Long> {

    public List<EcHistory> findTop50ByOrderByIdDesc();

    public List<EcHistory> findByChangeSummaryContainingOrSlugIs(String summaryKey, String slug);
}
