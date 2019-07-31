package org.madgik.services;

import org.madgik.rest.requests.PageableRequest;
import org.madgik.dtos.VisualizationExperimentDto;
import org.madgik.persistence.entities.VisualizationExperiment;
import org.madgik.persistence.repositories.VisualizationExperimentRepository;
import org.madgik.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service(Constants.VISUALIZATION_EXPERIMENT_SERVICE)
public class VisualizationExperimentService {

    @Autowired
    private VisualizationExperimentRepository repo;

    @Autowired
    private MapperService mapperService;

    public Page<VisualizationExperimentDto> getAllVisualizationExperiments(PageableRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize());
        Page<VisualizationExperiment> visualizationExperimentPage = repo.findAll(pageable);
        return visualizationExperimentPage.map(entity ->
                mapperService.getDto(entity, VisualizationExperimentDto.class));
    }
}
