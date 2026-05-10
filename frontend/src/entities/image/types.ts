export type ImageStatus = 'UPLOADED' | 'DELETED';

export type Image = {
  id: number;
  projectId: number;
  originalFileName: string;
  status: ImageStatus;
};
