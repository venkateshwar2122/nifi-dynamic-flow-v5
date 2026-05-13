package com.example.nifi.flow.planner;

import com.example.nifi.deployment.service.TablePipelineDeploymentService;
import com.example.nifi.flow.model.FlowContext;
import com.example.nifi.flow.template.ProcessorTemplate;
import com.example.nifi.flow.template.ProcessorTemplateRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DynamicPipelinePlanner {

    private final ProcessorTemplateRegistry templateRegistry;

    public DynamicPipelinePlanner(ProcessorTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    public PipelinePlan planTablePipeline(
            FlowContext ctx,
            TablePipelineDeploymentService.SharedControllerServices services
    ) {
        ProcessorTemplate sourceTemplate = templateRegistry.source(ctx.getSourceDbType());
        ProcessorTemplate destinationTemplate = templateRegistry.destination(ctx.getDestinationDbType());

        PlannedProcessor source = new PlannedProcessor(
                "source",
                ctx.getTableName() + "_source",
                0,
                sourceTemplate,
                Map.of(
                        sourceTemplate.getServiceProperties().get("dbcp"), services.dbcpId(),
                        "Table Name", ctx.getTableName(),
                        sourceTemplate.getServiceProperties().get("writer"), services.writerId(),
                        "Maximum-value Columns", "id"
                )
        );

        PlannedProcessor destination = new PlannedProcessor(
                "destination",
                ctx.getTableName() + "_destination",
                1,
                destinationTemplate,
                Map.of(
                        destinationTemplate.getServiceProperties().get("mongo"), services.mongoId(),
                        "Mongo Database Name", ctx.getDestinationDatabase(),
                        "Mongo Collection Name", ctx.getCollectionName(),
                        destinationTemplate.getServiceProperties().get("reader"), services.readerId(),
                        "update-key-fields", "id"
                )
        );

        return new PipelinePlan(
                List.of(source, destination),
                List.of(new PlannedConnection(source.getNodeId(), destination.getNodeId()))
        );
    }
}
