package com.hivemq.api.impl;

import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.api.resources.AuthenticationApi;
import com.hivemq.api.resources.BridgeApi;
import com.hivemq.api.resources.EventApi;
import com.hivemq.api.resources.FrontendApi;
import com.hivemq.api.resources.GatewayApi;
import com.hivemq.api.resources.GenericAPIHolder;
import com.hivemq.api.resources.HealthCheckApi;
import com.hivemq.api.resources.MetricsApi;
import com.hivemq.api.resources.ProtocolAdaptersApi;
import com.hivemq.api.resources.SamplingApi;
import com.hivemq.api.resources.TopicFilterApi;
import com.hivemq.api.resources.UnsApi;
import com.hivemq.api.resources.impl.RootResource;
import com.hivemq.edge.api.ApiApiService;
import com.hivemq.edge.api.ApiResponseMessage;
import com.hivemq.edge.api.NotFoundException;
import com.hivemq.edge.api.model.Adapter;
import com.hivemq.edge.api.model.AdapterConfig;
import com.hivemq.edge.api.model.ApiBearerToken;
import com.hivemq.edge.api.model.BehaviorPolicy;
import com.hivemq.edge.api.model.Bridge;
import com.hivemq.edge.api.model.DataPolicy;
import com.hivemq.edge.api.model.DomainTag;
import com.hivemq.edge.api.model.DomainTagList;
import com.hivemq.edge.api.model.ISA95ApiBean;
import com.hivemq.edge.api.model.NorthboundMappingList;
import com.hivemq.edge.api.model.Script;
import com.hivemq.edge.api.model.SouthboundMappingList;
import com.hivemq.edge.api.model.StatusTransitionCommand;
import com.hivemq.edge.api.model.TopicFilter;
import com.hivemq.edge.api.model.TopicFilterList;
import com.hivemq.edge.api.model.UsernamePasswordCredentials;
import dagger.Lazy;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJerseyServerCodegen", date = "2025-01-13T12:18:30.344109+01:00[Europe/Belgrade]", comments = "Generator version: 7.10.0")
public class ApiApiServiceImpl extends ApiApiService {


    private final @NotNull Lazy<MetricsApi> metricsApi;
    private final @NotNull Lazy<HealthCheckApi> healthCheckApi;
    private final @NotNull Lazy<AuthenticationApi> authenticationApi;
    private final @NotNull Lazy<BridgeApi> bridgeApi;
    private final @NotNull Lazy<MetricsApi> dashboardApi;
    private final @NotNull Lazy<ProtocolAdaptersApi> protocolAdaptersApi;
    private final @NotNull Lazy<UnsApi> unsApi;
    private final @NotNull Lazy<FrontendApi> frontendApi;
    private final @NotNull Lazy<GatewayApi> gatewayApi;
    private final @NotNull Lazy<EventApi> eventApi;
    private final @NotNull Lazy<RootResource> rootResource;
    private final @NotNull Lazy<Set<IAuthenticationHandler>> authenticationHandlers;
    private final @NotNull Lazy<ITokenGenerator> tokenGenerator;
    private final @NotNull Lazy<ITokenVerifier> tokenVerifier;
    private final @NotNull Lazy<GenericAPIHolder> genericAPIHolderLazy;
    private final @NotNull Lazy<SamplingApi> samplingResourceLazy;
    private final @NotNull Lazy<TopicFilterApi> topicFilterApiLazy;

    @Inject
    public ApiApiServiceImpl(
            final @NotNull Lazy<MetricsApi> metricsApi,
            final @NotNull Lazy<HealthCheckApi> healthCheckApi,
            final @NotNull Lazy<AuthenticationApi> authenticationApi,
            final @NotNull Lazy<BridgeApi> bridgeApi,
            final @NotNull Lazy<MetricsApi> dashboardApi,
            final @NotNull Lazy<ProtocolAdaptersApi> protocolAdaptersApi,
            final @NotNull Lazy<UnsApi> unsApi,
            final @NotNull Lazy<FrontendApi> frontendApi,
            final @NotNull Lazy<GatewayApi> gatewayApi,
            final @NotNull Lazy<EventApi> eventApi,
            final @NotNull Lazy<RootResource> rootResource,
            final @NotNull Lazy<Set<IAuthenticationHandler>> authenticationHandlers,
            final @NotNull Lazy<ITokenGenerator> tokenGenerator,
            final @NotNull Lazy<ITokenVerifier> tokenVerifier,
            final @NotNull Lazy<GenericAPIHolder> genericAPIHolderLazy,
            final @NotNull Lazy<SamplingApi> samplingResourceLazy,
            final @NotNull Lazy<TopicFilterApi> topicFilterApiLazy) {
        this.authenticationApi = authenticationApi;
        this.metricsApi = metricsApi;
        this.healthCheckApi = healthCheckApi;
        this.bridgeApi = bridgeApi;
        this.dashboardApi = dashboardApi;
        this.protocolAdaptersApi = protocolAdaptersApi;
        this.unsApi = unsApi;
        this.frontendApi = frontendApi;
        this.gatewayApi = gatewayApi;
        this.eventApi = eventApi;
        this.rootResource = rootResource;
        this.authenticationHandlers = authenticationHandlers;
        this.tokenGenerator = tokenGenerator;
        this.tokenVerifier = tokenVerifier;
        this.genericAPIHolderLazy = genericAPIHolderLazy;
        this.samplingResourceLazy = samplingResourceLazy;
        this.topicFilterApiLazy = topicFilterApiLazy;
    }



    @Override
    public Response addAdapter(String adapterType, Adapter adapter, SecurityContext securityContext) throws NotFoundException {
        return protocolAdaptersApi.get().addAdapter(adapterType, adapter);
    }

    @Override
    public Response addAdapterDomainTags(String adapterId, DomainTag domainTag, SecurityContext securityContext) throws NotFoundException {
        return protocolAdaptersApi.get().addAdapterDomainTag(adapterId, domainTag)
    }

    @Override
    public Response addBridge(Bridge bridge, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response addTopicFilters(TopicFilter topicFilter, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response authenticate(UsernamePasswordCredentials usernamePasswordCredentials, SecurityContext securityContext) throws NotFoundException {
        return authenticationApi.get().authenticate(usernamePasswordCredentials)
    }
    @Override
    public Response createBehaviorPolicy(BehaviorPolicy behaviorPolicy, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response createCompleteAdapter(String adaptertype, String adaptername, AdapterConfig adapterConfig, SecurityContext securityContext) throws NotFoundException {
        return protocolAdaptersApi.get().addCompleteAdapter(adaptertype, adaptername, adapterConfig);
    }
    @Override
    public Response createDataPolicy(DataPolicy dataPolicy, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response createSchema(Schema schema, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response createScript(Script script, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteAdapter(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteAdapterDomainTags(String adapterId, String tagName, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteBehaviorPolicy(String policyId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteDataPolicy(String policyId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteSchema(String schemaId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteScript(String scriptId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response deleteTopicFilter(String filter, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response discoverDataPoints(String adapterId, String root, Integer depth, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapter(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapterDomainTags(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapterNorthboundMappings(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapterSouthboundMappings(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapterStatus(String adapterId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapterTypes(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdapters(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdaptersForType(String adapterType, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAdaptersStatus(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAllBehaviorPolicies(String fields, String policyIds, String clientIds, Integer limit, String cursor, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAllDataPolicies(String fields, String policyIds, String schemaIds, String topic, Integer limit, String cursor, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAllSchemas(String fields, String types, String schemaIds, Integer limit, String cursor, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getAllScripts(String fields, String functionTypes, String scriptIds, Integer limit, String cursor, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getBehaviorPolicy(String policyId, String fields, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getBridgeByName(String bridgeId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getBridgeStatus(String bridgeId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getBridges(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getBridgesStatus(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getCapabilities(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getClientState(String clientId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getConfiguration(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getDataPolicy(String policyId, String fields, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getDomainTag(String tagName, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getDomainTags(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getEvents(Integer limit, Long since, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getFsms(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getFunctions(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getIsa95(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getListeners(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getMetrics(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getNorthboundMappings(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getNotifications(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getSample(String metricName, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getSamplesForTopic(String topic, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getSchema(String schemaId, String fields, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getSchemaForTopic(String topic, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getScript(String scriptId, String fields, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getSouthboundMappings(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getTagSchema(String protocolId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getTopicFilters(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getWritingSchema(String adapterId, String tagName, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response getXmlConfiguration(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response liveness(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response readiness(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response refreshToken(SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response removeBridge(String bridgeId, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response setIsa95(ISA95ApiBean isA95ApiBean, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response startSamplingForTopic(String topic, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response transitionAdapterStatus(String adapterId, StatusTransitionCommand statusTransitionCommand, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response transitionBridgeStatus(String bridgeId, StatusTransitionCommand statusTransitionCommand, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateAdapter(String adapterId, Adapter adapter, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateAdapterDomainTag(String adapterId, String tagName, DomainTag domainTag, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateAdapterDomainTags(String adapterId, DomainTagList domainTagList, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateAdapterNorthboundMappings(String adapterId, NorthboundMappingList northboundMappingList, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateAdapterSouthboundMappings(String adapterId, SouthboundMappingList southboundMappingList, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateBehaviorPolicy(String policyId, BehaviorPolicy behaviorPolicy, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateBridge(String bridgeId, Bridge bridge, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateDataPolicy(String policyId, DataPolicy dataPolicy, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateTopicFilter(String filter, TopicFilter topicFilter, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response updateTopicFilters(TopicFilterList topicFilterList, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response validateToken(ApiBearerToken apiBearerToken, SecurityContext securityContext) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
