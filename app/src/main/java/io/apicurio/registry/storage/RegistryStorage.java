/*
 * Copyright 2020 Red Hat
 * Copyright 2020 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.storage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.apicurio.common.apps.config.DynamicConfigPropertyDto;
import io.apicurio.common.apps.config.DynamicConfigStorage;
import io.apicurio.registry.content.ContentHandle;
import io.apicurio.registry.mt.TenantContext;
import io.apicurio.registry.storage.dto.ArtifactMetaDataDto;
import io.apicurio.registry.storage.dto.ArtifactReferenceDto;
import io.apicurio.registry.storage.dto.ArtifactSearchResultsDto;
import io.apicurio.registry.storage.dto.ArtifactVersionMetaDataDto;
import io.apicurio.registry.storage.dto.ContentWrapperDto;
import io.apicurio.registry.storage.dto.DownloadContextDto;
import io.apicurio.registry.storage.dto.EditableArtifactMetaDataDto;
import io.apicurio.registry.storage.dto.GroupMetaDataDto;
import io.apicurio.registry.storage.dto.LogConfigurationDto;
import io.apicurio.registry.storage.dto.OrderBy;
import io.apicurio.registry.storage.dto.OrderDirection;
import io.apicurio.registry.storage.dto.RoleMappingDto;
import io.apicurio.registry.storage.dto.RuleConfigurationDto;
import io.apicurio.registry.storage.dto.SearchFilter;
import io.apicurio.registry.storage.dto.StoredArtifactDto;
import io.apicurio.registry.storage.dto.VersionSearchResultsDto;
import io.apicurio.registry.storage.impexp.EntityInputStream;
import io.apicurio.registry.types.ArtifactState;
import io.apicurio.registry.types.ArtifactType;
import io.apicurio.registry.types.RuleType;
import io.apicurio.registry.utils.impexp.Entity;

/**
 * The artifactStore layer for the registry.
 *
 * @author eric.wittmann@gmail.com
 * @author Ales Justin
 */
public interface RegistryStorage extends DynamicConfigStorage {

    /**
     * The storage name
     */
    public String storageName();

    /**
     * Returns true if the storage implementation supports multitenancy.
     * If the storage supports multitenancy, it will get the tenant information from the {@link TenantContext}
     * @return if multitenancy is supported
     */
    public boolean supportsMultiTenancy();

    /**
     * Is the artifactStore ready?
     * <p>
     * By default we check if it can access list of global rules.
     *
     * @return true if yes, false if no
     */
    default boolean isReady() {
        return (getGlobalRules() != null);
    }

    /**
     * Is the artifactStore alive?
     * <p>
     * By default it's true.
     *
     * @return true if yes, false if no
     */
    default boolean isAlive() {
        return true;
    }

    /**
     * Update artifact state.
     * @param groupId (optional)
     * @param artifactId
     * @param state
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public void updateArtifactState(String groupId, String artifactId, ArtifactState state)
            throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Update artifact state.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @param state
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public void updateArtifactState(String groupId, String artifactId, String version, ArtifactState state)
            throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Creates a new artifact (from the given value) in the artifactStore.  The artifactId must be unique
     * within the given artifact group.  Returns a map of meta-data generated by the artifactStore layer, such as the
     * generated, globally unique globalId of the new value.
     * If the group did not exist previously it is created automatically.
     * @param groupId (optional)
     * @param artifactId
     * @param version (optional)
     * @param artifactType
     * @param content
     * @param references
     * @throws ArtifactAlreadyExistsException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto createArtifact(String groupId, String artifactId, String version, ArtifactType artifactType,
                                              ContentHandle content, List<ArtifactReferenceDto> references) throws ArtifactAlreadyExistsException, RegistryStorageException;

    /**
     * Creates a new artifact (from the given value including metadata) in the artifactStore.  The artifactId must be unique
     * within the given artifact group. Returns a map of meta-data generated by the artifactStore layer, such as the
     * generated, globally unique globalId of the new value.
     * @param groupId (optional)
     * @param artifactId
     * @param version (optional)
     * @param artifactType
     * @param content
     * @param metaData
     * @throws ArtifactAlreadyExistsException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto createArtifactWithMetadata(String groupId, String artifactId, String version,
            ArtifactType artifactType, ContentHandle content, EditableArtifactMetaDataDto metaData, List<ArtifactReferenceDto> references) throws ArtifactAlreadyExistsException, RegistryStorageException;

    /**
     * Deletes an artifact by its group and unique id. Returns list of artifact versions.
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public List<String> deleteArtifact(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Deletes all artifacts in the given group. DOES NOT delete the group.
     * @param groupId (optional)
     * @throws RegistryStorageException
     */
    public void deleteArtifacts(String groupId) throws RegistryStorageException;

    /**
     * Gets the most recent version of the value of the artifact with the given group and ID.
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public StoredArtifactDto getArtifact(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets some artifact content by the unique contentId.  This method of getting content
     * from storage does not allow extra meta-data to be returned, because the contentId only
     * points to a piece of content/data - it is divorced from any artifact version.
     * @param contentId
     * @throws ContentNotFoundException
     * @throws RegistryStorageException
     */
    public ContentWrapperDto getArtifactByContentId(long contentId) throws ContentNotFoundException, RegistryStorageException;

    /**
     * Gets some artifact content by the SHA-256 hash of that content.  This method of getting content
     * from storage does not allow extra meta-data to be returned, because the content hash only
     * points to a piece of content/data - it is divorced from any artifact version.
     * @param contentHash
     * @throws ContentNotFoundException
     * @throws RegistryStorageException
     */
    public ContentWrapperDto getArtifactByContentHash(String contentHash) throws ContentNotFoundException, RegistryStorageException;

    /**
     * Get artifact metadata for a given contentId
     * @param contentId
     * @return
     */
    public List<ArtifactMetaDataDto> getArtifactVersionsByContentId(long contentId);

    /**
     * Get artifact contentids for a given pair of coordinates
     * @param groupId
     * @param artifactId
     * @return
     */
    public List<Long> getArtifactContentIds(String groupId, String artifactId);

    /**
     * Updates the artifact value by storing the given value as a new version of the artifact.  Previous value
     * is NOT overwitten.  Returns a map of meta-data generated by the artifactStore layer, such as the generated,
     * globally unique globalId of the new value.
     * @param groupId (optional)
     * @param artifactId
     * @param version (optional)
     * @param artifactType
     * @param content
     * @param references
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto updateArtifact(String groupId, String artifactId, String version,
            ArtifactType artifactType, ContentHandle content, List<ArtifactReferenceDto> references) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Updates the artifact value by storing the given value and metadata as a new version of the artifact.  Previous value
     * is NOT overwitten.  Returns a map of meta-data generated by the artifactStore layer, such as the generated,
     * globally unique globalId of the new value.
     * @param groupId (optional)
     * @param artifactId
     * @param version (optional)
     * @param artifactType
     * @param content
     * @param metaData
     * @param references
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto updateArtifactWithMetadata(String groupId, String artifactId, String version,
            ArtifactType artifactType, ContentHandle content, EditableArtifactMetaDataDto metaData, List<ArtifactReferenceDto> references) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Get all artifact ids.
     * ---
     * Note: This should only be used in older APIs such as the registry V1 REST API and the Confluent API
     * ---
     * @return all artifact ids
     * @param limit the limit of artifacts
     */
    public Set<String> getArtifactIds(Integer limit);

    /**
     * Search artifacts by given criteria
     * @param filters the set of filters to apply when searching
     * @param orderBy the field to order by
     * @param orderDirection the direction to order the results
     * @param offset the number of artifacts to skip
     * @param limit the result size limit
     */
    public ArtifactSearchResultsDto searchArtifacts(Set<SearchFilter> filters, OrderBy orderBy, OrderDirection orderDirection,
            int offset, int limit);

    /**
     * Gets the stored meta-data for an artifact by group and ID.  This will include client-editable meta-data such as
     * name and description, but also generated meta-data such as "modifedOn" and "globalId".
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto getArtifactMetaData(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets the metadata of the version that matches content.
     * @param groupId (optional)
     * @param artifactId
     * @param canonical
     * @param content
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactVersionMetaDataDto getArtifactVersionMetaData(String groupId, String artifactId, boolean canonical,
            ContentHandle content) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets the stored meta-data for an artifact by global ID.  This will include client-editable meta-data such as
     * name and description, but also generated meta-data such as "modifedOn" and "globalId".
     * @param globalId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactMetaDataDto getArtifactMetaData(long globalId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Updates the stored meta-data for an artifact by group and ID.  Only the client-editable meta-data can be updated.  Client
     * editable meta-data includes e.g. name and description. TODO what if set to null?
     * @param groupId (optional)
     * @param artifactId
     * @param metaData
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public void updateArtifactMetaData(String groupId, String artifactId, EditableArtifactMetaDataDto metaData) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets a list of rules configured for a specific Artifact (by group and ID).  This will return only the names of the
     * rules.
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public List<RuleType> getArtifactRules(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Creates an artifact rule for a specific Artifact.  If the named rule already exists for the artifact, then
     * this should fail.
     * @param groupId (optional)
     * @param artifactId
     * @param rule
     * @param config
     * @throws ArtifactNotFoundException
     * @throws RuleAlreadyExistsException
     * @throws RegistryStorageException
     */
    public void createArtifactRule(String groupId, String artifactId, RuleType rule, RuleConfigurationDto config)
            throws ArtifactNotFoundException, RuleAlreadyExistsException, RegistryStorageException;

    /**
     * Deletes all rules stored/configured for the artifact.
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteArtifactRules(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets all of the information for a single rule configured on a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @param rule
     * @throws ArtifactNotFoundException
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public RuleConfigurationDto getArtifactRule(String groupId, String artifactId, RuleType rule)
            throws ArtifactNotFoundException, RuleNotFoundException, RegistryStorageException;

    /**
     * Updates the configuration information for a single rule on a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @param rule
     * @param config
     * @throws ArtifactNotFoundException
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public void updateArtifactRule(String groupId, String artifactId, RuleType rule, RuleConfigurationDto config)
            throws ArtifactNotFoundException, RuleNotFoundException, RegistryStorageException;

    /**
     * Deletes a single stored/configured rule for a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @param rule
     * @throws ArtifactNotFoundException
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteArtifactRule(String groupId, String artifactId, RuleType rule) throws ArtifactNotFoundException, RuleNotFoundException, RegistryStorageException;

    /**
     * Gets a sorted set of all artifact versions that exist for a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public List<String> getArtifactVersions(String groupId, String artifactId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Fetch the versions of the given artifact
     * @param groupId (optional)
     * @param artifactId the artifact used to fetch versions
     * @param limit the result size limit
     * @param offset the number of versions to skip
     * @return the artifact versions, limited
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public VersionSearchResultsDto searchVersions(String groupId, String artifactId, int offset, int limit) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets the stored artifact content for the artifact version with the given unique global ID.
     * @param globalId
     * @throws ArtifactNotFoundException
     * @throws RegistryStorageException
     */
    public StoredArtifactDto getArtifactVersion(long globalId) throws ArtifactNotFoundException, RegistryStorageException;

    /**
     * Gets the stored value for a single version of a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public StoredArtifactDto getArtifactVersion(String groupId, String artifactId, String version) throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Deletes a single version of a given artifact.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteArtifactVersion(String groupId, String artifactId, String version) throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Gets the stored meta-data for a single version of an artifact.  This will return all meta-data for the
     * version, including any user edited meta-data along with anything generated by the artifactStore.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public ArtifactVersionMetaDataDto getArtifactVersionMetaData(String groupId, String artifactId, String version) throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Updates the user-editable meta-data for a single version of a given artifact.  Only the client-editable
     * meta-data can be updated.  Client editable meta-data includes e.g. name and description.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @param metaData
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public void updateArtifactVersionMetaData(String groupId, String artifactId, String version, EditableArtifactMetaDataDto metaData) throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Deletes the user-editable meta-data for a singel version of a given artifact.  Only the client-editable
     * meta-data is deleted.  Any meta-data generated by the artifactStore is preserved.
     * @param groupId (optional)
     * @param artifactId
     * @param version
     * @throws ArtifactNotFoundException
     * @throws VersionNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteArtifactVersionMetaData(String groupId, String artifactId, String version) throws ArtifactNotFoundException, VersionNotFoundException, RegistryStorageException;

    /**
     * Gets a list of all global rule names.
     * @throws RegistryStorageException
     */
    public List<RuleType> getGlobalRules() throws RegistryStorageException;

    /**
     * Creates a single global rule.  Duplicates (by name) are not allowed.  Stores the rule name and configuration.
     * @param rule
     * @param config
     * @throws RuleAlreadyExistsException
     * @throws RegistryStorageException
     */
    public void createGlobalRule(RuleType rule, RuleConfigurationDto config) throws RuleAlreadyExistsException, RegistryStorageException;

    /**
     * Deletes all of the globally configured rules.
     * @throws RegistryStorageException
     */
    public void deleteGlobalRules() throws RegistryStorageException;

    /**
     * Gets all information about a single global rule.
     * @param rule
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public RuleConfigurationDto getGlobalRule(RuleType rule) throws RuleNotFoundException, RegistryStorageException;

    /**
     * Updates the configuration settings for a single global rule.
     * @param rule
     * @param config
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public void updateGlobalRule(RuleType rule, RuleConfigurationDto config) throws RuleNotFoundException, RegistryStorageException;

    /**
     * Deletes a single global rule.
     * @param rule
     * @throws RuleNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteGlobalRule(RuleType rule) throws RuleNotFoundException, RegistryStorageException;

    /**
     * Returns the log configuration persisted in the storage for the given logger
     * @param logger
     * @throws RegistryStorageException
     */
    public LogConfigurationDto getLogConfiguration(String logger) throws RegistryStorageException, LogConfigurationNotFoundException;

    /**
     * Persists the given log configuration
     * @param logConfiguration
     * @throws RegistryStorageException
     */
    public void setLogConfiguration(LogConfigurationDto logConfiguration) throws RegistryStorageException;

    /**
     * Removes the persisted log configuration for the given logger
     * @param logger
     * @throws RegistryStorageException
     */
    public void removeLogConfiguration(String logger) throws RegistryStorageException, LogConfigurationNotFoundException;

    /**
     * Returns the list of log configuration persisted in the storage
     * @throws RegistryStorageException
     */
    public List<LogConfigurationDto> listLogConfigurations() throws RegistryStorageException;

    /**
     * Creates a new empty group and stores it's metadata. When creating an artifact the group is automatically created in it does not exist.
     * @param group
     * @throws GroupAlreadyExistsException
     * @throws RegistryStorageException
     */
    public void createGroup(GroupMetaDataDto group) throws GroupAlreadyExistsException, RegistryStorageException;

    /**
     * Updates the metadata of an existent group.
     * @param group
     * @throws RegistryStorageException
     */
    public void updateGroupMetaData(GroupMetaDataDto group) throws GroupNotFoundException, RegistryStorageException;

    /**
     * Deletes a group intified by the given groupId and DELETES ALL resources related to this group
     * @param groupId (optional)
     * @throws GroupNotFoundException
     * @throws RegistryStorageException
     */
    public void deleteGroup(String groupId) throws GroupNotFoundException, RegistryStorageException;

    /**
     * Get all groupIds
     * @param limit
     * @throws RegistryStorageException
     */
    public List<String> getGroupIds(Integer limit) throws RegistryStorageException;

    /**
     * Get the metadata information for a group identified by the given groupId
     * @param groupId (optional)
     */
    public GroupMetaDataDto getGroupMetaData(String groupId) throws GroupNotFoundException, RegistryStorageException;

    /**
     * Called to export all data in the registry.  Caller provides a handle to handle the data/entities.  This
     * should be used to stream the data from the storage to some output source (e.g. a HTTP response).  It is
     * important that the full dataset is *not* kept in memory.
     * @param handler
     * @throws RegistryStorageException
     */
    public void exportData(Function<Entity, Void> handler) throws RegistryStorageException;

    /**
     * Called to import previously exported data into the registry.
     * @param entities
     * @param preserveGlobalId Preserve global ids. If false, global ids will be set to next id in global id sequence.
     * @param preserveContentId Preserve content id. If false, content ids will be set to the next ids in the content id sequence. Content-Version mapping will be preserved.
     * @throws RegistryStorageException
     */
    public void importData(EntityInputStream entities, boolean preserveGlobalId, boolean preserveContentId) throws RegistryStorageException;

    /**
     * Counts the total number of artifacts
     * @return artifacts count
     * @throws RegistryStorageException
     */
    public long countArtifacts() throws RegistryStorageException;

    /**
     * Counts the total number of versions for one artifact
     * @return
     * @throws RegistryStorageException
     */
    public long countArtifactVersions(String groupId, String artifactId) throws RegistryStorageException;

    /**
     * Counts the total number of versions for all artifacts
     * @return
     * @throws RegistryStorageException
     */
    public long countTotalArtifactVersions() throws RegistryStorageException;

    /**
     * Creates a role mapping for a user.
     * @param principalId
     * @param role
     * @param principalName
     */
    public void createRoleMapping(String principalId, String role, String principalName) throws RegistryStorageException;

    /**
     * Gets the list of all the role mappings in the registry.
     */
    public List<RoleMappingDto> getRoleMappings() throws RegistryStorageException;

    /**
     * Gets the details of a single role mapping.
     * @param principalId
     */
    public RoleMappingDto getRoleMapping(String principalId) throws RegistryStorageException;

    /**
     * Gets the role for a single user.  This returns null if there is no role mapped for
     * the given principal.
     * @param principalId
     */
    public String getRoleForPrincipal(String principalId) throws RegistryStorageException;

    /**
     * Updates a single role mapping.
     * @param principalId
     * @param role
     */
    public void updateRoleMapping(String principalId, String role) throws RegistryStorageException;

    /**
     * Deletes a single role mapping.
     * @param principalId
     */
    public void deleteRoleMapping(String principalId) throws RegistryStorageException;

    /**
     * Deletes ALL user (tenant) data. Does not delete global data, such as log configuration.
     */
    void deleteAllUserData();

    /**
     * Called to create a single-use download "link".  This can then be consumed using
     * "consumeDownload()".  Used to support browser flows for features like /admin/export.
     * @param context
     * @throws RegistryStorageException
     */
    public String createDownload(DownloadContextDto context) throws RegistryStorageException;

    /**
     * Called to consume a download from the DB (single-use) and return its context info.
     * @param downloadId
     */
    public DownloadContextDto consumeDownload(String downloadId) throws RegistryStorageException;

    /**
     * Called to delete any expired rows in the downloads table.  This is basically cleaning up
     * any single-use download links that were never "clicked".
     * @throws RegistryStorageException
     */
    public void deleteAllExpiredDownloads() throws RegistryStorageException;

    /**
     * Gets the raw value of a property, bypassing any caching that might be enabled.
     * @param propertyName the name of a property
     * @return the raw value
     */
    public DynamicConfigPropertyDto getRawConfigProperty(String propertyName);

    /**
     * Gets a list of tenantIds with stale configuration properties.  This would inform a caching
     * layer that a tenant-specific cache should be invalidated.
     * @param since instant representing the last time this check was done (has anything changed since)
     * @return a list of tenant IDs with stale configs
     */
    public List<String> getTenantsWithStaleConfigProperties(Instant since);


    /**
     * @return The artifact references resolved as a map containing the reference name as key and the referenced artifact content.
     */
    public Map<String, ContentHandle> resolveReferences(List<ArtifactReferenceDto> references);

    /**
     *
     * @param groupId
     * @param artifactId
     * @return true if an artifact exists with the coordinates passed as parameters
     * @throws RegistryStorageException
     */
    public boolean isArtifactExists(String groupId, String artifactId) throws RegistryStorageException;

    /**
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return list of content ids of schemas that references artifact
     */
    public List<Long> getContentIdsReferencingArtifact(String groupId, String artifactId, String version);
}
