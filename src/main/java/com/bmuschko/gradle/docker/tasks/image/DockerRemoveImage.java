/*
 * Copyright 2014 the original author or authors.
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
package com.bmuschko.gradle.docker.tasks.image;

import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.github.dockerjava.api.command.RemoveImageCmd;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.internal.provider.PropertyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.internal.lazy.Lazy;

import java.util.List;
import java.util.concurrent.Callable;

public class DockerRemoveImage extends AbstractDockerRemoteApiTask {
    private final ProviderFactory providers = getProject().getProviders();
    private final ObjectFactory objects = getProject().getObjects();
    private final Property<String> imageId = objects.property(String.class);

    private final void issueDeprecationWarning(String propertyName) {
        getLogger().warn("[DEPRECATED] DockerRemoveImage.{} is deprecated. Please use DockerRemoveImage.{}s.", propertyName, propertyName);
    }

//region Obsolete
    @Input
    @Optional
    @Deprecated
    public final Property<String> getImageId() {
        return imageId;

    }

    /**
     * [Deprecated] Sets the target image ID or name.
     *
     * @param imageId Image ID or name
     * @see #targetImageId(Callable)
     * @see #targetImageId(Provider)
     */
    @Deprecated
    public void targetImageId(String imageId) {

        issueDeprecationWarning("targetImageId");
        this.imageId.set(imageId);
    }

    /**
     * [Deprecated] Sets the target image ID or name.
     *
     * @param imageId Image ID or name as Callable
     * @see #targetImageId(String)
     * @see #targetImageId(Provider)
     */
    @Deprecated
    public void targetImageId(Callable<String> imageId) {

        issueDeprecationWarning("targetImageId");
        targetImageId(providers.provider(imageId));
    }
    /**
     * [Deprecated] Sets the target image ID or name.
     *
     * @param imageId Image ID or name as Provider
     * @see #targetImageId(String)
     * @see #targetImageId(Callable)
     */
    @Deprecated
    public void targetImageId(Provider<String> imageId) {
        issueDeprecationWarning("targetImageId");
        this.imageId.set(imageId);
    }
//endregion

    protected ListProperty<String> imageIds = objects.listProperty(java.lang.String.class);
    /**
     * Gets the target image IDs
     *
     */
    @Input
    @Optional
    public ListProperty<String> getTargetImageIds() {
        return imageIds;
    }


    public void setTargetImageIds(List<Property<String>> imageIds) {
        this.imageIds = objects.listProperty(java.lang.String.class);

        for (var id: imageIds) {
            this.imageIds.add(id);
        }
    }

    public void setTargetImageIds(Callable<List<String>> setImageIds) {
        setTargetImageIds(providers.provider(setImageIds));
    }

    public void setTargetImageIds(Provider<List<String>> setImageIds) {
        this.imageIds.set(setImageIds);
    }

    public void targetImageIds(Closure<List<String>> imageIds) {
        setTargetImageIds(providers.provider(imageIds));
    }

    public void targetImageIds(List<String> imageIds) {
        Callable<List<String>> task = () -> {
            return imageIds;
        };
        setTargetImageIds(task);
    }

    @Input
    @Optional
    public final Property<Boolean> getForce() {
        return force;
    }
    private final Property<Boolean> force = objects.property(Boolean.class);

    @Override
    public void runRemoteCommand() {
        if (null != this.imageId.getOrNull()) {
            getLogger().quiet("Removing image with ID '" + imageId.get() + ".");
            try (RemoveImageCmd removeImageCmd = getDockerClient().removeImageCmd(imageId.get())) {
                if (Boolean.TRUE.equals(force.getOrNull())) {
                    removeImageCmd.withForce(force.get());
                }
                removeImageCmd.exec();
            }
        } else {
            getLogger().warn("DockerRemoveImage: Id for remove image is empty.");
        }

        if (null != this.imageIds.getOrNull() && !this.imageIds.get().isEmpty()) {
            for (var id : this.imageIds.get()) {
                getLogger().quiet("Removing image with ID '" + id + ".");
                try (RemoveImageCmd removeImageCmd = getDockerClient().removeImageCmd(id)) {
                    if (Boolean.TRUE.equals(force.getOrNull())) {
                        removeImageCmd.withForce(force.get());
                    }
                    removeImageCmd.exec();
                }
            }
        } else {
            getLogger().warn("DockerRemoveImage: no image ids were set.");
        }
    }
}
