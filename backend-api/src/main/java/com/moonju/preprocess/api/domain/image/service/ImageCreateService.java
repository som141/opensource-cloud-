package com.moonju.preprocess.api.domain.image.service;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageCreateService {

    private final ImageRepository imageRepository;
    private final ImageArtifactRepository imageArtifactRepository;

    public ImageCreateService(
        ImageRepository imageRepository,
        ImageArtifactRepository imageArtifactRepository
    ) {
        this.imageRepository = imageRepository;
        this.imageArtifactRepository = imageArtifactRepository;
    }

    @Transactional
    public List<Image> createFromCompletedUpload(UploadSession uploadSession, List<UploadSessionFile> files) {
        List<Image> createdImages = new ArrayList<>();
        for (UploadSessionFile file : files) {
            if (imageRepository.existsByUploadSessionFileId(file.getId())) {
                continue;
            }
            Image image = imageRepository.save(Image.fromUpload(uploadSession, file));
            imageArtifactRepository.save(ImageArtifact.original(image));
            createdImages.add(image);
        }
        return createdImages;
    }
}
