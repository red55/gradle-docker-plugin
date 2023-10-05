package com.bmuschko.gradle.docker.tasks.image

import com.bmuschko.gradle.docker.AbstractGroovyDslFunctionalTest
import org.gradle.testkit.runner.BuildResult

class DockerRemoveImageFunctionalTest extends AbstractGroovyDslFunctionalTest {
    private static final String TEST_IMAGE2 = "alpine";
    private static final String TEST_IMAGE2_TAG = "3.18.3";
    public static final String TEST_IMAGE2_WITH_TAG = "${TEST_IMAGE2}:${TEST_IMAGE2_TAG}"

    def "can remove image"() {
        buildFile << """
            import com.bmuschko.gradle.docker.tasks.image.Dockerfile
            import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
            import com.bmuschko.gradle.docker.tasks.image.DockerListImages
            import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage

            task dockerfile(type: Dockerfile) {
                from '$TEST_IMAGE_WITH_TAG'
                label(['maintainer': 'jane.doe@example.com'])
            }

            task buildImage(type: DockerBuildImage) {
                dependsOn dockerfile
                inputDir = file("build/docker")
            }

            task removeImage(type: DockerRemoveImage) {
                dependsOn buildImage
                force = true
                targetImageId buildImage.getImageId()
            }

            task removeImageAndCheckRemoval(type: DockerListImages) {
                dependsOn removeImage
                showAll = true
                dangling = true
            }
        """

        when:
        BuildResult result = build('removeImageAndCheckRemoval')

        then:
        result.output.contains("[DEPRECATED]")
        !result.output.contains("repository")
    }

    def "can remove image tagged in multiple repositories"() {
        buildFile << """
            import com.bmuschko.gradle.docker.tasks.image.Dockerfile
            import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
            import com.bmuschko.gradle.docker.tasks.image.DockerListImages
            import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
            import com.bmuschko.gradle.docker.tasks.image.DockerTagImage

            task dockerfile(type: Dockerfile) {
                from '$TEST_IMAGE_WITH_TAG'
                label(['maintainer': 'jane.doe@example.com'])
            }

            task buildImage(type: DockerBuildImage) {
                dependsOn dockerfile
                inputDir = file("build/docker")
            }

            task tagImage(type: DockerTagImage) {
                dependsOn buildImage
                repository = "repository"
                tag = "tag2"
                targetImageId buildImage.getImageId()
            }

            task tagImageSecondTime(type: DockerTagImage) {
                dependsOn tagImage
                repository = "repository"
                tag = "tag2"
                targetImageId buildImage.getImageId()
            }

            task removeImage(type: DockerRemoveImage) {
                dependsOn tagImageSecondTime
                force = true
                targetImageId buildImage.getImageId()
            }

            task removeImageAndCheckRemoval(type: DockerListImages) {
                dependsOn removeImage
                showAll = true
                dangling = true
            }
        """

        when:
        BuildResult result = build('removeImageAndCheckRemoval')

        then:
        result.output.contains("[DEPRECATED]")
        !result.output.contains("repository")
    }

    def "can remove multiple images (ids from List<Property<T>>)"() {
        buildFile << """
                import com.bmuschko.gradle.docker.tasks.image.Dockerfile
                import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
                import com.bmuschko.gradle.docker.tasks.image.DockerListImages
                import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
                import com.bmuschko.gradle.docker.tasks.image.DockerTagImage

                task dockerfile1(type: Dockerfile) {
                    from '$TEST_IMAGE_WITH_TAG'
                    label(['maintainer': 'jane.doe@example.com', 'test':'can remove multiple images (ids from List<Property<T>>)'])
                }

                task buildImage1(type: DockerBuildImage) {
                    dependsOn dockerfile1
                    inputDir = file("build/docker")
                }

                task dockerfile2(type: Dockerfile) {
                    from '$TEST_IMAGE2_WITH_TAG'
                    label(['maintainer': 'john.doe@example.com', 'test':'can remove multiple images (ids from List<Property<T>>)'])
                }

                task buildImage2(type: DockerBuildImage) {
                    dependsOn dockerfile2
                    inputDir = file("build/docker")
                }

                task removeImage(type: DockerRemoveImage) {
                    dependsOn buildImage1, buildImage2
                    force = true

                    targetImageIds = [buildImage1.imageId, buildImage2.imageId]

                }

                task removeImageAndCheckRemoval(type: DockerListImages) {
                    dependsOn removeImage
                    labels = ['test':'can remove multiple images (ids from List<Property<T>>)']
                    dangling = true
                }

        """
        when:
        BuildResult result = build('removeImageAndCheckRemoval')

        then:
        !result.output.contains("[DEPRECATED]")
        !result.output.contains("Repository Tags :")
    }

    def "can remove multiple images (ids from List<String>)"() {
        buildFile << """
                import com.bmuschko.gradle.docker.tasks.image.Dockerfile
                import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
                import com.bmuschko.gradle.docker.tasks.image.DockerListImages
                import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
                import com.bmuschko.gradle.docker.tasks.image.DockerTagImage

                tasks.register('dockerfile1', Dockerfile) {
                    from '$TEST_IMAGE_WITH_TAG'
                    label(['maintainer': 'jane.doe@example.com', 'test':'can remove multiple images (ids from List<String>)'])
                }

                tasks.register('buildImage1', DockerBuildImage) {
                    dependsOn dockerfile1
                    inputDir = file("build/docker")
                }

                tasks.register('dockerfile2', Dockerfile) {
                    from '$TEST_IMAGE2_WITH_TAG'
                    label(['maintainer': 'john.doe@example.com', 'test':'can remove multiple images (ids from List<String>)'])
                }

                tasks.register('buildImage2', DockerBuildImage) {
                    dependsOn dockerfile2
                    inputDir = file("build/docker")
                }

                def images = []
                tasks.register('listImages', DockerListImages) {
                    dependsOn tasks.named('buildImage1'), tasks.named('buildImage2')
                    labels = ['test':'can remove multiple images (ids from List<String>)']
                    dangling = true
                    onNext {
                        i -> images.add(i.Id)
                    }
                }

                tasks.register('removeImage', DockerRemoveImage) {
                    dependsOn tasks.named('listImages')
                    force = true

                    targetImageIds {
                        images
                    }
                }

                tasks.register('removeImageAndCheckRemoval', DockerListImages) {
                    dependsOn tasks.named('removeImage')
                    labels = ['test':'can remove multiple images (ids from List<String>)']
                    dangling = true
                }

        """
        when:
        BuildResult result = build('removeImageAndCheckRemoval')

        then:
        !result.output.contains("[DEPRECATED]")
        !result.output.contains("Repository Tags :")
    }
}
