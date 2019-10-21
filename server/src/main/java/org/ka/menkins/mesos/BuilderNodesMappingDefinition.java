package org.ka.menkins.mesos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BuilderNodesMappingDefinition {
    List<BuilderNodeDefinition> builders;
}
