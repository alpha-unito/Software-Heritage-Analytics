<div x-data="" wire:poll.5s>
    <x-table>
        <x-slot name="head">
            <x-table.heading sortable>{{ __('Application') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Recipe') }}</x-table.heading>
            <x-table.heading class="whitespace-nowrap" sortable>{{ __('Creation Date') }}</x-table.heading>
            <x-table.heading class="whitespace-nowrap" sortable>{{ __('Evaluation Date') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Settings') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Action') }}</x-table.heading>
        </x-slot>
        <x-slot name="body">
            @forelse ($runs as $run)
                <x-table.row class="border-b">
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $run->application->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $run->recipe->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell class="whitespace-nowrap">
                        {{ $run->created_at->format("m/d/Y") }}
                    </x-table.cell>
                    <x-table.cell  class="whitespace-nowrap">
                        @if($run->path != '')
                            {{ explode(' ',$run->execution_time)[1] }}
                        @endif
                    </x-table.cell>                    
                    <x-table.cell class="w-full">
                        {{ $run->settings }}
                    </x-table.cell>

                    <x-table.cell>
                        <div class="flex flex-row space-x-2">
                            @if(is_null($run->job_id))
                            <x-button wire:click="run({{ $run->id }})" type="button" color="green">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-play class="w-3 font-green-700" />
                                    <div>Run</div>
                                </div>
                            </x-button>
                            @elseif(is_null($run->path))
                            <div class="inline-flex items-center px-3 py-2 text-sm font-medium leading-4 text-green-700 bg-white border border-green-300 rounded-md shadow-sm">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-loading class="w-4 font-green-700 animate-spin" />
                                    <div>Calculating</div>
                                </div>
                            </div>
                            @else
                            <x-button wire:click="inspect({{ $run->id }})" type="button" color="green">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-inspect class="w-4 font-green-700" />
                                    <div>Inspect</div>
                                </div>
                            </x-button>
                            @endif
                            <x-button type="button"
                                x-on:click="$wire.delete({{ $run->id }}); $dispatch('delete-alert', {id: {{ $run->id }}})"
                                color="red">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-delete class="w-3 font-red-700" />
                                    <label>Delete</label>
                                </div>
                            </x-button>
                        </div>
                    </x-table.cell>
                </x-table.row>
            @empty
                <x-table.row class="border-b">
                    <x-table.cell colspan="7">
                        <div class="flex justify-center font-semibold text-gray-700">
                            {{ __('Nothing to display') }}
                        </div>
                    </x-table.cell>
                </x-table.row>
            @endforelse
        </x-slot>
    </x-table>
</div>
