import React, { useState, useEffect } from 'react';
import { X, Settings, Calendar, RefreshCw } from 'lucide-react';
import { InventoryItem, ScheduleType, AutoOrderConfigDTO } from '../../types';

interface AutoRestockModalProps {
    item: InventoryItem | null;
    isOpen: boolean;
    onClose: () => void;
    onSave: (config: Partial<AutoOrderConfigDTO>) => void;
}

const AutoRestockModal: React.FC<AutoRestockModalProps> = ({ item, isOpen, onClose, onSave }) => {
    // Default values
    const [threshold, setThreshold] = useState(10);
    const [reorderQuantity, setReorderQuantity] = useState(25);
    const [scheduleType, setScheduleType] = useState<ScheduleType>(ScheduleType.THRESHOLD_BASED);
    const [intervalDays, setIntervalDays] = useState(7);
    const [dayOfWeek, setDayOfWeek] = useState('MONDAY');
    const [dayOfMonth, setDayOfMonth] = useState('1');

    useEffect(() => {
        if (item?.autoOrderConfig) {
            setThreshold(item.autoOrderConfig.minThreshold || 10);
            setReorderQuantity(item.autoOrderConfig.reorderQuantity || 25);
            setScheduleType(item.autoOrderConfig.scheduleType || ScheduleType.THRESHOLD_BASED);
            setIntervalDays(item.autoOrderConfig.intervalDays || 7);
            setDayOfWeek(item.autoOrderConfig.dayOfWeek || 'MONDAY');
            setDayOfMonth(item.autoOrderConfig.dayOfMonth || '1');
        }
    }, [item]);

    if (!isOpen || !item) return null;

    const handleSave = () => {
        const config: Partial<AutoOrderConfigDTO> = {
            minThreshold: threshold,
            reorderQuantity: reorderQuantity,
            scheduleType: scheduleType,
            intervalDays: scheduleType === ScheduleType.INTERVAL_DAYS ? intervalDays : null,
            dayOfWeek: scheduleType === ScheduleType.WEEKLY ? dayOfWeek : null,
            dayOfMonth: scheduleType === ScheduleType.MONTHLY ? dayOfMonth : null,
            enabled: true
        };
        onSave(config);
        onClose();
    };

    const daysOfWeek = [
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
    ];

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="bg-gradient-to-r from-primary-600 to-primary-700 p-6 text-white shrink-0">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
                                <Settings className="w-5 h-5" />
                            </div>
                            <div>
                                <h2 className="text-lg font-semibold">Auto-Restock Configuration</h2>
                                <p className="text-primary-100 text-sm">{item.name}</p>
                            </div>
                        </div>
                        <button
                            onClick={onClose}
                            className="w-8 h-8 bg-white/20 hover:bg-white/30 rounded-lg flex items-center justify-center transition-colors"
                        >
                            <X className="w-4 h-4" />
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="p-6 space-y-6 overflow-y-auto">
                    {/* Strategy Selector */}
                    <div>
                        <label className="label">Restock Strategy</label>
                        <div className="grid grid-cols-1 gap-2">
                            <button
                                className={`p-3 rounded-lg border text-left transition-all ${scheduleType === ScheduleType.THRESHOLD_BASED ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:border-gray-300'}`}
                                onClick={() => setScheduleType(ScheduleType.THRESHOLD_BASED)}
                            >
                                <div className="font-medium flex items-center gap-2">
                                    <Settings className="w-4 h-4" />
                                    Threshold Based
                                </div>
                                <p className="text-xs opacity-80 mt-1">Reorder when stock falls below a specific level</p>
                            </button>

                            <button
                                className={`p-3 rounded-lg border text-left transition-all ${scheduleType === ScheduleType.WEEKLY ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:border-gray-300'}`}
                                onClick={() => setScheduleType(ScheduleType.WEEKLY)}
                            >
                                <div className="font-medium flex items-center gap-2">
                                    <Calendar className="w-4 h-4" />
                                    Weekly Schedule
                                </div>
                                <p className="text-xs opacity-80 mt-1">Reorder on a specific day every week</p>
                            </button>

                            <button
                                className={`p-3 rounded-lg border text-left transition-all ${scheduleType === ScheduleType.MONTHLY ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:border-gray-300'}`}
                                onClick={() => setScheduleType(ScheduleType.MONTHLY)}
                            >
                                <div className="font-medium flex items-center gap-2">
                                    <Calendar className="w-4 h-4" />
                                    Monthly Schedule
                                </div>
                                <p className="text-xs opacity-80 mt-1">Reorder on a specific day every month</p>
                            </button>

                            <button
                                className={`p-3 rounded-lg border text-left transition-all ${scheduleType === ScheduleType.INTERVAL_DAYS ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 hover:border-gray-300'}`}
                                onClick={() => setScheduleType(ScheduleType.INTERVAL_DAYS)}
                            >
                                <div className="font-medium flex items-center gap-2">
                                    <RefreshCw className="w-4 h-4" />
                                    Fixed Interval
                                </div>
                                <p className="text-xs opacity-80 mt-1">Reorder every X days</p>
                            </button>
                        </div>
                    </div>

                    {/* Dynamic Fields based on Strategy */}
                    <div className="space-y-4 pt-4 border-t border-gray-100">
                        <h3 className="font-medium text-navy-800">Configuration Details</h3>

                        {scheduleType === ScheduleType.THRESHOLD_BASED && (
                            <div>
                                <label className="label">Minimum Threshold</label>
                                <div className="relative">
                                    <input
                                        type="number"
                                        value={threshold}
                                        onChange={(e) => setThreshold(Number(e.target.value))}
                                        className="input pr-16"
                                        min="1"
                                    />
                                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-navy-500 text-sm">units</span>
                                </div>
                                <p className="text-xs text-navy-500 mt-1">Trigger reorder when stock is less than this</p>
                            </div>
                        )}

                        {scheduleType === ScheduleType.WEEKLY && (
                            <div>
                                <label className="label">Day of Week</label>
                                <select
                                    className="input"
                                    value={dayOfWeek}
                                    onChange={(e) => setDayOfWeek(e.target.value)}
                                >
                                    {daysOfWeek.map(d => (
                                        <option key={d} value={d}>{d}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        {scheduleType === ScheduleType.MONTHLY && (
                            <div>
                                <label className="label">Day of Month</label>
                                <select
                                    className="input"
                                    value={dayOfMonth}
                                    onChange={(e) => setDayOfMonth(e.target.value)}
                                >
                                    {Array.from({ length: 28 }, (_, i) => i + 1).map(d => (
                                        <option key={d} value={d.toString()}>{d}{[1, 21, 31].includes(d) ? 'st' : [2, 22].includes(d) ? 'nd' : [3, 23].includes(d) ? 'rd' : 'th'}</option>
                                    ))}
                                </select>
                            </div>
                        )}

                        {scheduleType === ScheduleType.INTERVAL_DAYS && (
                            <div>
                                <label className="label">Interval (Days)</label>
                                <div className="relative">
                                    <input
                                        type="number"
                                        value={intervalDays}
                                        onChange={(e) => setIntervalDays(Number(e.target.value))}
                                        className="input pr-16"
                                        min="1"
                                    />
                                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-navy-500 text-sm">days</span>
                                </div>
                            </div>
                        )}

                        {/* Reorder Quantity - Limit to all types */}
                        <div>
                            <label className="label">Reorder Quantity</label>
                            <div className="relative">
                                <input
                                    type="number"
                                    value={reorderQuantity}
                                    onChange={(e) => setReorderQuantity(Number(e.target.value))}
                                    className="input pr-16"
                                    min="1"
                                />
                                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-navy-500 text-sm">units</span>
                            </div>
                            <p className="text-xs text-navy-500 mt-1">Amount to order when triggered</p>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="px-6 py-4 bg-gray-50 flex gap-3 shrink-0">
                    <button onClick={onClose} className="flex-1 btn-secondary">Cancel</button>
                    <button onClick={handleSave} className="flex-1 btn-primary">Save Configuration</button>
                </div>
            </div>
        </div>
    );
};

export default AutoRestockModal;
