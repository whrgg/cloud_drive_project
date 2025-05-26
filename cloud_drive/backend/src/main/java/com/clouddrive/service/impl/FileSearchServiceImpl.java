package com.clouddrive.service.impl;

import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.service.FileSearchService;
import com.clouddrive.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件搜索服务实现类
 */
@Service
@Slf4j
public class FileSearchServiceImpl implements FileSearchService {

    private static final String FILE_INDEX = "cloud_drive_files";

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private FileService fileService;

    /**
     * 初始化索引
     */
    @PostConstruct
    public void init() {
        try {
            // 检查索引是否存在
            boolean exists = elasticsearchClient.indices().exists(
                    new ExistsRequest.Builder().index(FILE_INDEX).build()
            ).value();
            
            if (!exists) {
                // 创建索引
                CreateIndexResponse response = elasticsearchClient.indices().create(c -> c
                    .index(FILE_INDEX)
                    .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("1")
                    )
                    .mappings(m -> m
                        .properties("fileName", p -> p
                            .text(t -> t
                                .analyzer("ik_max_word")
                                .searchAnalyzer("ik_smart")
                            )
                        )
                        .properties("fileType", p -> p
                            .keyword(k -> k)
                        )
                        .properties("userId", p -> p
                            .long_(l -> l)
                        )
                        .properties("isDir", p -> p
                            .boolean_(b -> b)
                        )
                        .properties("parentId", p -> p
                            .long_(l -> l)
                        )
                        .properties("createTime", p -> p
                            .date(d -> d
                                .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
                            )
                        )
                        .properties("updateTime", p -> p
                            .date(d -> d
                                .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
                            )
                        )
                        .properties("content", p -> p
                            .text(t -> t
                                .analyzer("ik_max_word")
                                .searchAnalyzer("ik_smart")
                            )
                        )
                    )
                );
                
                log.info("Created Elasticsearch index: {}, acknowledged: {}", FILE_INDEX, response.acknowledged());
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index", e);
        }
    }

    @Override
    public boolean indexFile(UserFile userFile) {
        try {
            // 转换UserFile对象为Map
            Map<String, Object> document = convertUserFileToMap(userFile);
            
            // 创建索引请求
            IndexResponse response = elasticsearchClient.index(i -> i
                .index(FILE_INDEX)
                .id(userFile.getId().toString())
                .document(document)
            );
            
            log.debug("Indexed file: {}, result: {}", userFile.getId(), response.result());
            return true;
        } catch (IOException e) {
            log.error("Failed to index file: {}", userFile.getId(), e);
            return false;
        }
    }

    @Override
    public boolean bulkIndexFiles(List<UserFile> userFiles) {
        try {
            // 准备批量操作
            List<BulkOperation> operations = new ArrayList<>();
            
            for (UserFile userFile : userFiles) {
                // 转换UserFile对象为Map
                Map<String, Object> document = convertUserFileToMap(userFile);
                
                // 创建索引操作
                operations.add(new BulkOperation.Builder()
                    .index(i -> i
                        .index(FILE_INDEX)
                        .id(userFile.getId().toString())
                        .document(document)
                    )
                    .build()
                );
            }
            
            // 执行批量操作
            BulkResponse response = elasticsearchClient.bulk(b -> b
                .index(FILE_INDEX)
                .operations(operations)
            );
            
            // 检查是否有失败的操作
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("Failed to index file: {}, error: {}", item.id(), item.error().reason());
                    }
                }
                return false;
            }
            
            return true;
        } catch (IOException e) {
            log.error("Failed to bulk index files", e);
            return false;
        }
    }

    @Override
    public boolean updateFileIndex(UserFile userFile) {
        try {
            // 转换UserFile对象为Map
            Map<String, Object> document = convertUserFileToMap(userFile);
            
            // 创建更新请求
            UpdateResponse<Object> response = elasticsearchClient.update(u -> u
                .index(FILE_INDEX)
                .id(userFile.getId().toString())
                .doc(document)
                .docAsUpsert(true)
            , Object.class);
            
            log.debug("Updated file index: {}, result: {}", userFile.getId(), response.result());
            return true;
        } catch (IOException e) {
            log.error("Failed to update file index: {}", userFile.getId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteFileIndex(Long fileId) {
        try {
            // 创建删除请求
            DeleteResponse response = elasticsearchClient.delete(d -> d
                .index(FILE_INDEX)
                .id(fileId.toString())
            );
            
            log.debug("Deleted file index: {}, result: {}", fileId, response.result());
            return true;
        } catch (IOException e) {
            log.error("Failed to delete file index: {}", fileId, e);
            return false;
        }
    }

    @Override
    public List<UserFile> searchFiles(String keyword, Long userId) {
        return searchFiles(keyword, userId, 0, 100);
    }

    @Override
    public List<UserFile> searchFiles(String keyword, Long userId, int page, int size) {
        try {
            // 创建搜索请求
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                .index(FILE_INDEX)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m
                            .term(t -> t
                                .field("userId")
                                .value(userId)
                            )
                        )
                        .must(m -> m
                            .bool(sb -> sb
                                .should(s1 -> s1
                                    .match(m1 -> m1
                                        .field("fileName")
                                        .query(keyword)
                                    )
                                )
                                .should(s2 -> s2
                                    .match(m2 -> m2
                                        .field("content")
                                        .query(keyword)
                                    )
                                )
                            )
                        )
                        .mustNot(mn -> mn
                            .term(t -> t
                                .field("isDeleted")
                                .value(true)
                            )
                        )
                    )
                )
                .from(page * size)
                .size(size)
            , Map.class);
            
            // 处理搜索结果
            List<Hit<Map>> hits = response.hits().hits();
            List<UserFile> results = new ArrayList<>();
            
            for (Hit<Map> hit : hits) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;
                
                // 从ES搜索结果构建UserFile对象
                UserFile userFile = new UserFile();
                userFile.setId(Long.valueOf(hit.id()));
                userFile.setFileName((String) source.get("fileName"));
                userFile.setFileType((String) source.get("fileType"));
                userFile.setUserId(userId);
                userFile.setIsDir((Boolean) source.get("isDir"));
                
                Object parentIdObj = source.get("parentId");
                if (parentIdObj != null) {
                    if (parentIdObj instanceof Integer) {
                        userFile.setParentId(((Integer) parentIdObj).longValue());
                    } else if (parentIdObj instanceof Long) {
                        userFile.setParentId((Long) parentIdObj);
                    } else {
                        userFile.setParentId(Long.valueOf(parentIdObj.toString()));
                    }
                }
                
                // 添加其他需要的字段
                Object fileIdObj = source.get("fileId");
                if (fileIdObj != null) {
                    if (fileIdObj instanceof Integer) {
                        userFile.setFileId(((Integer) fileIdObj).longValue());
                    } else if (fileIdObj instanceof Long) {
                        userFile.setFileId((Long) fileIdObj);
                    } else {
                        userFile.setFileId(Long.valueOf(fileIdObj.toString()));
                    }
                }
                
                Object fileSizeObj = source.get("fileSize");
                if (fileSizeObj != null) {
                    if (fileSizeObj instanceof Integer) {
                        userFile.setFileSize(((Integer) fileSizeObj).longValue());
                    } else if (fileSizeObj instanceof Long) {
                        userFile.setFileSize((Long) fileSizeObj);
                    } else {
                        userFile.setFileSize(Long.valueOf(fileSizeObj.toString()));
                    }
                }
                
                results.add(userFile);
            }
            
            return results;
        } catch (IOException e) {
            log.error("Failed to search files", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将UserFile对象转换为Map
     */
    private Map<String, Object> convertUserFileToMap(UserFile userFile) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", userFile.getId());
        map.put("fileName", userFile.getFileName());
        map.put("userId", userFile.getUserId());
        map.put("isDir", userFile.getIsDir());
        map.put("parentId", userFile.getParentId());
        map.put("createTime", userFile.getCreateTime());
        map.put("updateTime", userFile.getUpdateTime());
        map.put("isDeleted", userFile.getDelFlag() == 1);
        
        // 如果是文件，添加文件相关信息
        if (!userFile.getIsDir() && userFile.getFileId() != null) {
            map.put("fileId", userFile.getFileId());
            
            // 获取文件详情
            File file = fileService.getFileById(userFile.getFileId());
            if (file != null) {
                map.put("fileSize", file.getFileSize());
                map.put("fileType", file.getFileType());
                // 可以添加文件内容提取逻辑，但需要额外实现
                // map.put("content", extractFileContent(file));
            }
        }
        
        return map;
    }
} 