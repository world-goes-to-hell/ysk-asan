import { Outlet } from 'react-router-dom';

import Header from './Header';
import Sidebar from './Sidebar';
import styles from '../../styles/layout.module.css';

export default function AppLayout() {
  return (
    <div className={styles.shell}>
      <Header />
      <div className={styles.body}>
        <Sidebar />
        <main className={styles.main}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
