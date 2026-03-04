import { RouterProvider } from 'react-router-dom';
import MotionLayer from './components/MotionLayer';
import { router } from './routes';

export default function App(): JSX.Element {
  return (
    <MotionLayer>
      <RouterProvider router={router} />
    </MotionLayer>
  );
}
