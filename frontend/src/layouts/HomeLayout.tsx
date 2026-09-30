import { Outlet } from 'react-router-dom';
import Header from '../components/Header';

function HomeLayout() {
  return (
    <div className="min-h-screen bg-stone-50">
      <Header />
      <Outlet />
    </div>
  );
}

export default HomeLayout;
