import { useState } from 'react';

import AutocompleteInput from '../common/AutocompleteInput';
import { departmentSuggestions, emailSuggestions } from '../../utils/suggestions';
import styles from '../../styles/contacts.module.css';

export default function ContactAddForm({ onAdd, departments = [], knownEmails = [] }) {
  const [department, setDepartment] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!department.trim() || !name.trim() || !email.trim()) return;
    setSubmitting(true);
    try {
      await onAdd({
        department: department.trim(),
        name: name.trim(),
        email: email.trim(),
      });
      setDepartment('');
      setName('');
      setEmail('');
    } catch (_) {
      // 상위에서 토스트 처리 — 입력은 유지(재시도 편의)
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className={styles.addForm} onSubmit={onSubmit}>
      <AutocompleteInput
        value={department}
        onChange={setDepartment}
        getSuggestions={(v) => departmentSuggestions(v, departments)}
        placeholder="부서"
        aria-label="부서"
        required
        maxLength={100}
      />
      <input
        className="form-input"
        type="text"
        placeholder="이름"
        value={name}
        onChange={(e) => setName(e.target.value)}
        aria-label="이름"
        required
        maxLength={100}
      />
      <AutocompleteInput
        value={email}
        onChange={setEmail}
        getSuggestions={(v) => emailSuggestions(v, knownEmails)}
        type="email"
        placeholder="이메일"
        aria-label="이메일"
        required
        maxLength={255}
      />
      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? '추가 중…' : '추가'}
      </button>
    </form>
  );
}
