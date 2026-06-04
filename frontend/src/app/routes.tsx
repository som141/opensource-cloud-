import { DashboardPage } from '../pages/DashboardPage';
import { ImageDetailPage } from '../pages/ImageDetailPage';
import { JobDetailPage } from '../pages/JobDetailPage';
import { LoginPage } from '../pages/LoginPage';
import { OAuthSuccessPage } from '../pages/OAuthSuccessPage';
import { ProjectDetailPage } from '../pages/ProjectDetailPage';
import { ProjectListPage } from '../pages/ProjectListPage';
import { UploadPage } from '../pages/UploadPage';

export type AppRoute = {
  path: string;
  label: string;
  element: JSX.Element;
  showInNav?: boolean;
  eyebrow?: string;
};

export const routes: AppRoute[] = [
  { path: '/', label: 'Dashboard', element: <DashboardPage />, eyebrow: 'Operations overview' },
  { path: '/login', label: 'Login', element: <LoginPage />, showInNav: false },
  { path: '/oauth2/success', label: 'OAuth Success', element: <OAuthSuccessPage />, showInNav: false },
  { path: '/projects', label: 'Projects', element: <ProjectListPage />, eyebrow: 'Workspace scope' },
  { path: '/projects/:projectId', label: 'Project Detail', element: <ProjectDetailPage />, eyebrow: 'Project workspace' },
  { path: '/upload', label: 'Upload', element: <UploadPage />, eyebrow: 'Batch processing' },
  { path: '/jobs/:jobId', label: 'Job Detail', element: <JobDetailPage />, eyebrow: 'Job monitoring' },
  { path: '/images/:imageId', label: 'Image Detail', element: <ImageDetailPage />, eyebrow: 'Image artifact' }
];

export function resolveRoute(pathname: string): AppRoute {
  const exactMatch = routes.find((route) => route.path === pathname);
  if (exactMatch) {
    return exactMatch;
  }

  if (pathname.startsWith('/projects/')) {
    return routes.find((route) => route.path === '/projects/:projectId') ?? routes[0];
  }
  if (pathname.startsWith('/jobs/')) {
    return routes.find((route) => route.path === '/jobs/:jobId') ?? routes[0];
  }
  if (pathname.startsWith('/images/')) {
    return routes.find((route) => route.path === '/images/:imageId') ?? routes[0];
  }

  return routes[0];
}
