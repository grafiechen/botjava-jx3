package com.grafie.botjava.mapper;

import com.grafie.botjava.entity.ScriptStatusFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptStatusFieldDefinitionMapper extends JpaRepository<ScriptStatusFieldDefinition, Long> {

    ScriptStatusFieldDefinition findByMongoFieldName(String mongoFieldName);

    List<ScriptStatusFieldDefinition> findByEnabledTrueOrderByGroupNameAscSortOrderAscIdAsc();

    List<ScriptStatusFieldDefinition> findAllByOrderByGroupNameAscSortOrderAscIdAsc();
}